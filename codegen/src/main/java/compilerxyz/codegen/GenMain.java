package compilerxyz.codegen;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.palantir.javaformat.java.Formatter;
import com.palantir.javaformat.java.FormatterException;
import com.palantir.javaformat.java.JavaFormatterOptions;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import compilerxyz.codegen.CodecsGen.Component;
import compilerxyz.codegen.CodecsGen.TypeKind;
import compilerxyz.codegen.experimental.BuiltinIdentityScanner;
import compilerxyz.codegen.experimental.BuiltinPathScanner;
import compilerxyz.codegen.experimental.CodegenDescriptorScanner;
import compilerxyz.codegen.experimental.DynamicTypeScanner;
import compilerxyz.codegen.experimental.JavaAspectExtractor;
import compilerxyz.codegen.util.BuiltinMethodEmit;
import compilerxyz.codegen.util.BuiltinMethodEmit.Method;
import compilerxyz.codegen.util.BuiltinMethodEmit.Param;
import compilerxyz.codegen.util.DynamicTypes;
import compilerxyz.codegen.util.NameDerivation;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import compilerxyz.prelude.util.TOrderedSet;
import compilerxyz.prelude.util.TSet;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;

public final class GenMain {
    private GenMain() {}

    static {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    }

    public static final String GEN_PACKAGE = "compilerxyz.main.gen";
    public static final ClassName TYPE = ClassName.get("compilerxyz.main.eval", "Type");
    public static final ClassName OPT = ClassName.get("compilerxyz.prelude.util", "Opt");
    public static final ClassName UNIQUE = ClassName.get("compilerxyz.main.eval", "Unique");
    public static final ClassName UNIQUE_BUILTIN = ClassName.get("compilerxyz.main.eval", "Unique", "Builtin");
    public static final ClassName STATIC_METHOD_REF =
            ClassName.get("compilerxyz.main.eval.dynamic", "JavaStaticMethodRef");
    public static final ClassName VIRTUAL_METHOD_REF =
            ClassName.get("compilerxyz.main.eval.dynamic", "JavaVirtualMethodRef");
    public static final ClassName INTERNAL_NAME = ClassName.get("compilerxyz.main.eval.dynamic", "JavaInternalName");
    public static final ClassName METHOD_DESCRIPTOR =
            ClassName.get("compilerxyz.main.eval.dynamic", "JavaMethodDescriptor");
    public static final ClassName DATA_DESCRIPTOR =
            ClassName.get("compilerxyz.main.eval.dynamic", "JavaDataDescriptor");

    public record IdentitySpec(String name, long builtinUniqueId, boolean standalone) {}

    private static TList<Path> sourceFiles(Path... roots) throws IOException {
        var files = new TList<Path>();
        for (var root : roots) {
            try (Stream<Path> stream = Files.walk(root)) {
                files.addAll(stream.filter(p -> p.toString().endsWith(".java")).toList());
            }
        }
        return files;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Expected three args: output source root, runtime source root, util source root");
        }
        var outRoot = Path.of(args[0]);
        var runtimeSourceRoot = Path.of(args[1]);
        var utilSourceRoot = Path.of(args[2]);
        var dispatchRoots = new TSet<String>();
        if (Files.exists(outRoot)) {
            try (var walk = Files.walk(outRoot)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        }
        Files.createDirectories(outRoot);

        var formatter = Formatter.createFormatter(JavaFormatterOptions.builder()
                .style(JavaFormatterOptions.Style.PALANTIR)
                .build());

        var aspectIds = new TSet<Long>();
        var scopeIds = new TSet<Long>();

        TMap<ClassName, CodeBlock> scanRoot1;

        _scanRoot:
        {
            var out = new TMap<ClassName, CodeBlock>();
            {
                var files = sourceFiles(runtimeSourceRoot, utilSourceRoot);
                for (var file : files) {
                    var src = Files.readString(file);
                    if (!src.contains("dynamicType") && !src.contains("@Builtin")) continue;
                    var cu = StaticJavaParser.parse(file);
                    var pkg = cu.getPackageDeclaration()
                            .map(pd -> pd.getNameAsString())
                            .orElse("");
                    for (var t : cu.getTypes()) {

                        var simple = t.getNameAsString();
                        var cn = ClassName.get(pkg, simple);
                        DynamicTypeScanner.putDescriptor(out, cn, "Gen" + simple, t);
                        for (var member : t.getMembers()) {
                            if (member instanceof TypeDeclaration<?> nested) {
                                DynamicTypeScanner.walkNested(pkg, simple, nested, out);
                            }
                        }
                    }
                }
            }
            var mirror = new TMap<ClassName, CodeBlock>();
            for (var e : out.entrySet()) {
                var cn = e.getKey();
                if (!cn.packageName().equals("compilerxyz.handwritten")) continue;
                if (cn.simpleNames().size() < 2) continue;
                var mirrored = ClassName.get(
                        "compilerxyz.main.gen",
                        cn.simpleNames().get(0),
                        cn.simpleNames().subList(1, cn.simpleNames().size()).toArray(String[]::new));
                mirror.put(mirrored, e.getValue());
            }
            for (var e : mirror.entrySet()) out.put(e.getKey(), e.getValue());
            {
                scanRoot1 = out;
                break _scanRoot;
            }
        }

        TMap<ClassName, CodeBlock> scannedLeaves = scanRoot1;
        scannedLeaves.put(
                ClassName.get("compilerxyz.main.eval", "Code"),
                CodeBlock.of("new $T()", ClassName.get("compilerxyz.main.eval.value.code", "TypeCodeDynamic")));
        {
            var cls = TypeSpec.classBuilder("GenDynamicTypes").addModifiers(Modifier.PUBLIC, Modifier.FINAL);
            cls.addMethod(MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .build());

            var method = MethodSpec.methodBuilder("byClassName")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(ParameterizedTypeName.get(OPT, TYPE))
                    .addParameter(String.class, "name");
            var names = new ArrayList<ClassName>();
            for (var e : scannedLeaves.entrySet()) names.add(e.getKey());
            names.sort(Comparator.comparing(ClassName::reflectionName));
            method.beginControlFlow("switch (name)");
            for (var cn : names) {
                method.addStatement(
                        "case $S: return $T.some($L)",
                        cn.reflectionName(),
                        OPT,
                        scannedLeaves.getOpt(cn).optUnwrap());
            }
            method.addStatement("default: return $T.none()", OPT);
            method.endControlFlow();
            cls.addMethod(method.build());

            var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
            GenMain.write(outRoot, file, formatter);
        }

        JavaAspectExtractor.ScanResult scanRoot2;

        _scanRoot:
        {
            var builtins = new TList<BuiltinInterfacesGen.BuiltinSpec>();
            {
                var files = sourceFiles(runtimeSourceRoot, utilSourceRoot);
                for (var file : files) {
                    var src = Files.readString(file);
                    if (!src.contains("@BuiltinFull(")) continue;
                    var cu = StaticJavaParser.parse(file);
                    Opt<TypeDeclaration<?>> findBuiltinType;
                    _findBuiltinType:
                    {
                        for (var t : cu.getTypes()) {
                            var hasBuiltin =
                                    t.getAnnotationByName("BuiltinFull").isPresent()
                                            || t.getAnnotationByName("compilerxyz.prelude.annotations.BuiltinFull")
                                                    .isPresent();
                            if (!hasBuiltin) continue;
                            if (t instanceof ClassOrInterfaceDeclaration c) {
                                findBuiltinType = Opt.some(c);
                                break _findBuiltinType;
                            }
                            if (t instanceof EnumDeclaration en) {
                                findBuiltinType = Opt.some(en);
                                break _findBuiltinType;
                            }
                            if (t instanceof RecordDeclaration r) {
                                findBuiltinType = Opt.some(r);
                                break _findBuiltinType;
                            }
                        }
                        {
                            findBuiltinType = Opt.none();
                            break _findBuiltinType;
                        }
                    }
                    Opt<TypeDeclaration<?>> declOpt = findBuiltinType;

                    if (declOpt.optIsSome()) {
                        var decl = declOpt.optUnwrap();
                        Opt<BuiltinInterfacesGen.BuiltinSpec> builtin;
                        if (decl instanceof ClassOrInterfaceDeclaration coid) {
                            if (coid.isInterface()) {
                                ClassOrInterfaceDeclaration ifaceDecl = coid;
                                boolean interfaceExtendsAspect;
                                _interfaceExtendsAspect:
                                {
                                    for (var ext : coid.getExtendedTypes()) {
                                        var n = ext.getNameAsString();
                                        if (n.equals("Aspect") || n.equals("compilerxyz.main.eval.aspect.Aspect")) {
                                            interfaceExtendsAspect = true;
                                            break _interfaceExtendsAspect;
                                        }
                                    }
                                    {
                                        interfaceExtendsAspect = false;
                                        break _interfaceExtendsAspect;
                                    }
                                }
                                boolean isAspect = interfaceExtendsAspect;

                                Opt<BuiltinInterfacesGen.BuiltinSpec> extractBuiltinSpecInterface;
                                _extractBuiltinSpecInterface:
                                {
                                    var simpleName = ifaceDecl.getNameAsString();
                                    var pkg = cu.getPackageDeclaration()
                                            .orElseThrow()
                                            .getNameAsString();
                                    var builtinAnn = JavaAspectExtractor.getBuiltinAnnotation(ifaceDecl);
                                    var attrs = JavaAspectExtractor.readAttrs(builtinAnn);
                                    var uniqueIdStr = attrs.get("uniqueId");
                                    if (uniqueIdStr == null) {
                                        throw new IllegalStateException(
                                                "@Builtin on " + simpleName + " missing required uniqueId");
                                    }
                                    var uniqueId = Long.parseLong(uniqueIdStr);
                                    var identityIdStr = attrs.get("identityId");
                                    var identityId = identityIdStr == null ? 0L : Long.parseLong(identityIdStr);
                                    if (isAspect && identityId == 0L) {
                                        throw new IllegalStateException(
                                                "@Builtin on aspect " + simpleName + " missing required identityId");
                                    }
                                    var importMap = JavaAspectExtractor.buildImportMap(cu);
                                    var effectiveImports = new TMap<String, String>();
                                    for (var e : importMap.entrySet()) effectiveImports.put(e.getKey(), e.getValue());
                                    if (isAspect) {
                                        for (var member : ifaceDecl.getMembers()) {
                                            if (member instanceof RecordDeclaration r) {
                                                effectiveImports.put(
                                                        r.getNameAsString(),
                                                        pkg + "." + simpleName + "." + r.getNameAsString());
                                            }
                                        }
                                    }
                                    var methods = new TList<Method>();
                                    for (var m : ifaceDecl.getMethods()) {
                                        if (!m.isAbstract()) continue;
                                        String javaMethodName = m.getNameAsString();
                                        String interfaceSimpleName = simpleName;
                                        String deriveSourceName;
                                        _deriveSourceName:
                                        {
                                            var prefix = Character.toLowerCase(interfaceSimpleName.charAt(0))
                                                    + interfaceSimpleName.substring(1);
                                            if (javaMethodName.startsWith(prefix)
                                                    && javaMethodName.length() > prefix.length()) {
                                                var rest = javaMethodName.substring(prefix.length());
                                                {
                                                    deriveSourceName =
                                                            Character.toLowerCase(rest.charAt(0)) + rest.substring(1);
                                                    break _deriveSourceName;
                                                }
                                            }
                                            {
                                                deriveSourceName = javaMethodName;
                                                break _deriveSourceName;
                                            }
                                        }
                                        var sourceName = NameDerivation.snakeCase(deriveSourceName);
                                        var params = new TList<Param>();
                                        for (var p : m.getParameters()) {
                                            String name = p.getNameAsString();
                                            String stripReservedSuffix;
                                            _stripReservedSuffix:
                                            {
                                                if (name.endsWith("_") && name.length() > 1) {
                                                    stripReservedSuffix = name.substring(0, name.length() - 1);
                                                    break _stripReservedSuffix;
                                                }
                                                {
                                                    stripReservedSuffix = name;
                                                    break _stripReservedSuffix;
                                                }
                                            }
                                            params.add(new Param(
                                                    NameDerivation.snakeCase(stripReservedSuffix),
                                                    JavaAspectExtractor.typeNameOf(
                                                            p.getType(), effectiveImports, pkg)));
                                        }
                                        methods.add(new Method(
                                                sourceName,
                                                m.getNameAsString(),
                                                params,
                                                JavaAspectExtractor.typeNameOf(m.getType(), effectiveImports, pkg),
                                                true,
                                                false));
                                    }
                                    {
                                        extractBuiltinSpecInterface = Opt.some(new BuiltinInterfacesGen.BuiltinSpec(
                                                simpleName,
                                                pkg,
                                                uniqueId,
                                                identityId,
                                                isAspect,
                                                methods,
                                                new TList<BuiltinInterfacesGen.DataField>()));
                                        break _extractBuiltinSpecInterface;
                                    }
                                }
                                builtin = extractBuiltinSpecInterface;

                            } else {
                                builtin = JavaAspectExtractor.extractBuiltinSpecClass(cu, coid, Opt.none());
                            }
                        } else if (decl instanceof RecordDeclaration recDecl) {
                            builtin = JavaAspectExtractor.extractBuiltinSpecRecord(cu, recDecl, Opt.none());
                        } else if (decl instanceof EnumDeclaration enumDecl) {
                            builtin = JavaAspectExtractor.extractBuiltinSpecClass(cu, enumDecl, Opt.none());
                        } else {
                            throw new IllegalStateException(
                                    "JavaAspectExtractor: unexpected decl type " + decl.getClass());
                        }
                        if (builtin.optIsSome()) builtins.add(builtin.optUnwrap());
                    }
                    for (var t : cu.getTypes()) {
                        JavaAspectExtractor.collectNestedBuiltinRecords(file, cu, t, builtins);
                    }
                }
            }
            {
                scanRoot2 = new JavaAspectExtractor.ScanResult(builtins);
                break _scanRoot;
            }
        }

        JavaAspectExtractor.ScanResult scan = scanRoot2;

        {
            TList<BuiltinInterfacesGen.BuiltinSpec> specs = scan.builtins();

            TMap<ClassName, CodeBlock> leafDescriptors = scannedLeaves;

            var seenNames = new TSet<String>();
            var identityIds = new TSet<Long>();
            for (var spec : specs) {
                if (!seenNames.add(spec.simpleName())) {
                    throw new IllegalStateException("duplicate builtin interface name: " + spec.simpleName());
                }
                if (!aspectIds.add(spec.uniqueId())) {
                    throw new IllegalStateException("duplicate aspect id " + spec.uniqueId() + " (in builtins)");
                }
                if (spec.identityId() != 0L && !identityIds.add(spec.identityId())) {
                    throw new IllegalStateException(
                            "duplicate aspect identity id " + spec.identityId() + " (in builtins)");
                }

                var companionName = "Gen" + spec.simpleName();
                var self = ClassName.get(BuiltinInterfacesGen.GEN_PACKAGE, companionName);
                ClassName interfaceRef;
                if (spec.outerSimpleName().optIsSome()
                        && spec.nestedSimpleName().optIsSome()) {
                    interfaceRef = ClassName.get(
                            spec.pkg(),
                            spec.outerSimpleName().optUnwrap(),
                            spec.nestedSimpleName().optUnwrap());
                } else {
                    interfaceRef = ClassName.get(spec.pkg(), spec.simpleName());
                }

                var cls = TypeSpec.classBuilder(companionName).addModifiers(Modifier.PUBLIC, Modifier.FINAL);
                cls.addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .build());

                if (spec.isAspect()) {
                    cls.addField(FieldSpec.builder(
                                    BuiltinInterfacesGen.UNIQUE,
                                    "AspectId",
                                    Modifier.PUBLIC,
                                    Modifier.STATIC,
                                    Modifier.FINAL)
                            .initializer("new $T($LL)", BuiltinInterfacesGen.UNIQUE_BUILTIN, spec.uniqueId())
                            .build());
                    cls.addField(FieldSpec.builder(
                                    BuiltinInterfacesGen.UNIQUE,
                                    "IdentityId",
                                    Modifier.PUBLIC,
                                    Modifier.STATIC,
                                    Modifier.FINAL)
                            .initializer("new $T($LL)", BuiltinInterfacesGen.UNIQUE_BUILTIN, spec.identityId())
                            .build());
                    cls.addMethod(MethodSpec.methodBuilder("getMaybe")
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .returns(ParameterizedTypeName.get(BuiltinInterfacesGen.OPT, interfaceRef))
                            .addParameter(BuiltinInterfacesGen.EXPR_CONTEXT, "xx")
                            .addParameter(BuiltinInterfacesGen.TYPE, "t")
                            .addStatement(
                                    "return $T.getMaybe(xx, t, AspectId, $T.class)",
                                    BuiltinInterfacesGen.ASPECT_LOOKUP,
                                    interfaceRef)
                            .build());
                    cls.addMethod(MethodSpec.methodBuilder("get")
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .returns(ParameterizedTypeName.get(BuiltinInterfacesGen.ERESULT, interfaceRef))
                            .addParameter(BuiltinInterfacesGen.EXPR_CONTEXT, "xx")
                            .addParameter(BuiltinInterfacesGen.QEXPR_ID, "loc")
                            .addParameter(BuiltinInterfacesGen.TYPE, "t")
                            .addStatement(
                                    "return $T.get(xx, loc, t, AspectId, $T.class)",
                                    BuiltinInterfacesGen.ASPECT_LOOKUP,
                                    interfaceRef)
                            .build());
                }

                cls.addMethod(MethodSpec.methodBuilder("dynamicType")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(BuiltinInterfacesGen.TYPE_OBJECT_DYNAMIC)
                        .addStatement("return Holder.INSTANCE")
                        .build());

                var holder =
                        TypeSpec.classBuilder("Holder").addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
                holder.addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .build());
                holder.addField(FieldSpec.builder(
                                BuiltinInterfacesGen.TYPE_OBJECT_DYNAMIC,
                                "INSTANCE",
                                Modifier.PUBLIC,
                                Modifier.STATIC,
                                Modifier.FINAL)
                        .initializer("build()")
                        .build());
                MethodSpec buildBuild;
                _buildBuild:
                {
                    var typeDesc = spec.isAspect()
                            ? "aspect_"
                                    + BuiltinInterfacesGen.snake(
                                            spec.simpleName().substring("Aspect".length()))
                            : BuiltinInterfacesGen.snake(spec.simpleName());

                    var b = MethodSpec.methodBuilder("build")
                            .addModifiers(spec.isAspect() ? Modifier.PUBLIC : Modifier.PRIVATE, Modifier.STATIC)
                            .returns(BuiltinInterfacesGen.TYPE_OBJECT_DYNAMIC);

                    b.addStatement(
                            "var jvmClass = $T.fromClass($T.class)",
                            BuiltinInterfacesGen.JAVA_INTERNAL_NAME,
                            interfaceRef);
                    b.addStatement(
                            "var jvmClassDesc = $T.fromClass($T.class)",
                            BuiltinInterfacesGen.JAVA_DATA_DESCRIPTOR,
                            interfaceRef);
                    b.addStatement(
                            "var castRules = new $T<$T>()",
                            BuiltinInterfacesGen.TLIST,
                            BuiltinInterfacesGen.TYPE_OBJECT_CAST_RULE);
                    b.addStatement(
                            "var trivialIdentityAspects = new $T<$T>()",
                            BuiltinInterfacesGen.TLIST,
                            BuiltinInterfacesGen.UNIQUE);
                    if (spec.isAspect()) {
                        b.addStatement("trivialIdentityAspects.add($T.IdentityId)", self);
                        b.addStatement(
                                "var meta = new $T($S, $T.from(trivialIdentityAspects), $T.from(castRules), $T.<$T>of(), "
                                        + "$T.<$T>some(new $T($T.AspectId, $T.IdentityId)), false)",
                                BuiltinInterfacesGen.TYPE_OBJECT_META,
                                typeDesc,
                                BuiltinInterfacesGen.ILIST,
                                BuiltinInterfacesGen.ILIST,
                                BuiltinInterfacesGen.ILIST,
                                BuiltinInterfacesGen.TYPE_OBJECT_CAST_FROM_RULE,
                                BuiltinInterfacesGen.OPT,
                                BuiltinInterfacesGen.OBJECT_ASPECT_SOURCE,
                                BuiltinInterfacesGen.OBJECT_ASPECT_SOURCE_STATIC,
                                self,
                                self);
                        b.addStatement(
                                "var instance = new $T(meta, jvmClass, jvmClassDesc, $T.class, "
                                        + "$T.DYNAMIC, $T.some(jvmClass))",
                                BuiltinInterfacesGen.TYPE_OBJECT_DYNAMIC,
                                interfaceRef,
                                BuiltinInterfacesGen.OBJECT_DROP_KIND,
                                BuiltinInterfacesGen.OPT);
                    } else if (spec.isRecord()) {
                        b.addStatement(
                                "trivialIdentityAspects.add(new $T($LL))",
                                BuiltinInterfacesGen.UNIQUE_BUILTIN,
                                spec.uniqueId());
                        BuiltinInterfacesGen.emitTypeHostRestamp(b, interfaceRef);
                        b.addStatement(
                                "var meta = new $T($S, $T.from(trivialIdentityAspects), $T.from(castRules), $T.<$T>of(), "
                                        + "$T.<$T>none(), false)",
                                BuiltinInterfacesGen.TYPE_OBJECT_META,
                                typeDesc,
                                BuiltinInterfacesGen.ILIST,
                                BuiltinInterfacesGen.ILIST,
                                BuiltinInterfacesGen.ILIST,
                                BuiltinInterfacesGen.TYPE_OBJECT_CAST_FROM_RULE,
                                BuiltinInterfacesGen.OPT,
                                BuiltinInterfacesGen.OBJECT_ASPECT_SOURCE);
                        b.addStatement(
                                "var instance = new $T(meta, jvmClass, jvmClassDesc, $T.class, "
                                        + "$T.NONE, $T.<$T>none())",
                                BuiltinInterfacesGen.TYPE_OBJECT_DYNAMIC,
                                interfaceRef,
                                BuiltinInterfacesGen.OBJECT_DROP_KIND,
                                BuiltinInterfacesGen.OPT,
                                BuiltinInterfacesGen.JAVA_INTERNAL_NAME);
                    } else {
                        b.addStatement(
                                "trivialIdentityAspects.add(new $T($LL))",
                                BuiltinInterfacesGen.UNIQUE_BUILTIN,
                                spec.uniqueId());
                        BuiltinInterfacesGen.emitTypeHostRestamp(b, interfaceRef);
                        b.addStatement(
                                "var meta = new $T($S, $T.from(trivialIdentityAspects), $T.from(castRules), $T.<$T>of(), "
                                        + "$T.<$T>some(new $T(new $T($LL), new $T($LL))), $L)",
                                BuiltinInterfacesGen.TYPE_OBJECT_META,
                                typeDesc,
                                BuiltinInterfacesGen.ILIST,
                                BuiltinInterfacesGen.ILIST,
                                BuiltinInterfacesGen.ILIST,
                                BuiltinInterfacesGen.TYPE_OBJECT_CAST_FROM_RULE,
                                BuiltinInterfacesGen.OPT,
                                BuiltinInterfacesGen.OBJECT_ASPECT_SOURCE,
                                BuiltinInterfacesGen.OBJECT_ASPECT_SOURCE_STATIC,
                                BuiltinInterfacesGen.UNIQUE_BUILTIN,
                                spec.uniqueId(),
                                BuiltinInterfacesGen.UNIQUE_BUILTIN,
                                spec.uniqueId(),
                                spec.identityToken());
                        if (spec.isInterface()) {
                            b.addStatement(
                                    "var instance = new $T(meta, jvmClass, jvmClassDesc, $T.class, "
                                            + "$T.NONE, $T.some(jvmClass))",
                                    BuiltinInterfacesGen.TYPE_OBJECT_DYNAMIC,
                                    interfaceRef,
                                    BuiltinInterfacesGen.OBJECT_DROP_KIND,
                                    BuiltinInterfacesGen.OPT);
                        } else {
                            b.addStatement(
                                    "var instance = new $T(meta, jvmClass, jvmClassDesc, $T.class, "
                                            + "$T.NONE, $T.<$T>none())",
                                    BuiltinInterfacesGen.TYPE_OBJECT_DYNAMIC,
                                    interfaceRef,
                                    BuiltinInterfacesGen.OBJECT_DROP_KIND,
                                    BuiltinInterfacesGen.OPT,
                                    BuiltinInterfacesGen.JAVA_INTERNAL_NAME);
                        }
                    }

                    b.addStatement("return instance");
                    {
                        buildBuild = b.build();
                        break _buildBuild;
                    }
                }
                holder.addMethod(buildBuild);
                cls.addType(holder.build());

                MethodSpec buildLink;

                _buildLink:
                {
                    var b = MethodSpec.methodBuilder("link").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
                    b.addStatement("var instance = Holder.INSTANCE");
                    b.beginControlFlow("if (instance.isLinked())");
                    b.addStatement("return");
                    b.endControlFlow();
                    b.addStatement(
                            "var methods = new $T<$T>()",
                            BuiltinInterfacesGen.TLIST,
                            BuiltinInterfacesGen.OBJECT_METHOD);
                    b.addStatement(
                            "var dataFields = new $T<$T>()",
                            BuiltinInterfacesGen.TLIST,
                            BuiltinInterfacesGen.OBJECT_DATA_FIELD);
                    for (var m : spec.methods()) {
                        ClassName hostClass = interfaceRef;
                        String receiverVar = "instance";
                        String methodsVar = "methods";
                        BuiltinMethodEmit.Method method = m;
                        _emitObjectMethod:
                        {
                            var where = spec.simpleName() + "." + method.hostMethodName();
                            var paramDescs = new TList<CodeBlock>();
                            var paramRawClasses = new TList<TypeName>();
                            for (var p : method.params()) {
                                var pdOpt = BuiltinMethodEmit.resolveDescriptor(p.type(), leafDescriptors);
                                if (pdOpt.optIsNone()) {
                                    throw new IllegalStateException("codegen: no language type for parameter "
                                            + p.name() + ": " + p.type() + " of " + where);
                                }
                                paramDescs.add(pdOpt.optUnwrap());
                                var rawOpt = BuiltinMethodEmit.rawClass(p.type());
                                if (rawOpt.optIsNone()) {
                                    throw new IllegalStateException(
                                            "codegen: no raw class for parameter " + p.name() + " of " + where);
                                }
                                paramRawClasses.add(rawOpt.optUnwrap());
                            }
                            var retRawOpt = BuiltinMethodEmit.rawClass(method.ret());
                            if (retRawOpt.optIsNone()) {
                                throw new IllegalStateException("codegen: no raw class for return of " + where);
                            }
                            var retRaw = retRawOpt.optUnwrap();

                            var suffix = method.hostMethodName();

                            var argRecordName = "params_" + suffix;
                            var paramsExpr = CodeBlock.builder();
                            paramsExpr.add("$T.<$T>of(", BuiltinMethodEmit.ILIST, BuiltinMethodEmit.FUNCTION_PARAM);
                            for (int i = 0; i < method.params().size(); i++) {
                                var p = method.params().get(i);
                                if (i > 0) paramsExpr.add(", ");
                                paramsExpr.add(
                                        "new $T($S, $L)",
                                        BuiltinMethodEmit.FUNCTION_PARAM,
                                        p.name(),
                                        paramDescs.get(i));
                            }
                            paramsExpr.add(")");
                            b.addStatement("var $L = $L", argRecordName, paramsExpr.build());

                            var retTypeName = "retType_" + suffix;
                            if (method.ret().equals(TypeName.VOID)) {
                                b.addStatement(
                                        "var $L = $T.<$T>none()",
                                        retTypeName,
                                        BuiltinMethodEmit.OPT,
                                        BuiltinMethodEmit.TYPE);
                            } else {
                                var retDescOpt = BuiltinMethodEmit.resolveDescriptor(method.ret(), leafDescriptors);
                                if (retDescOpt.optIsNone()) {
                                    throw new IllegalStateException("codegen: no language type for return type "
                                            + method.ret() + " of " + where);
                                }
                                var retDesc = retDescOpt.optUnwrap();
                                b.addStatement(
                                        "var $L = $T.<$T>some($L)",
                                        retTypeName,
                                        BuiltinMethodEmit.OPT,
                                        BuiltinMethodEmit.TYPE,
                                        retDesc);
                            }

                            var descName = "desc_" + suffix;
                            var argClassBuilder = CodeBlock.builder();
                            if (method.needsEvalContext()) {
                                argClassBuilder.add(
                                        "$T.fromClass($T.class)",
                                        BuiltinMethodEmit.JAVA_DATA_DESCRIPTOR,
                                        ClassName.get("compilerxyz.main.eval", "EvalContext"));
                            }
                            for (int i = 0; i < paramRawClasses.size(); i++) {
                                if (i > 0 || method.needsEvalContext()) argClassBuilder.add(", ");
                                argClassBuilder.add(
                                        "$T.fromClass($T.class)",
                                        BuiltinMethodEmit.JAVA_DATA_DESCRIPTOR,
                                        paramRawClasses.get(i));
                            }
                            b.addStatement(
                                    "var $L = $T.fromParts($T.fromClass($T.class), $T.of($L))",
                                    descName,
                                    BuiltinMethodEmit.JAVA_METHOD_DESCRIPTOR,
                                    BuiltinMethodEmit.JAVA_DATA_DESCRIPTOR,
                                    retRaw,
                                    BuiltinMethodEmit.TLIST,
                                    argClassBuilder.build());

                            var jvmClassExpr = CodeBlock.of(
                                    "$T.fromClass($T.class)",
                                    ClassName.get("compilerxyz.main.eval.dynamic", "JavaInternalName"),
                                    hostClass);
                            var methodVar = "method_" + suffix;
                            dispatchRoots.add(hostClass.reflectionName() + "#" + method.hostMethodName());
                            b.addStatement(
                                    "var $L = new $T($N, $L, $L, $L, $S, $L, $T.class.isInterface(), $L)",
                                    methodVar,
                                    BuiltinMethodEmit.TYPE_METHOD_DYNAMIC,
                                    receiverVar,
                                    argRecordName,
                                    retTypeName,
                                    jvmClassExpr,
                                    method.hostMethodName(),
                                    descName,
                                    hostClass,
                                    method.needsEvalContext());
                            b.addStatement(
                                    "$L.add(new $T($S, $L))",
                                    methodsVar,
                                    BuiltinMethodEmit.OBJECT_METHOD,
                                    method.sourceName(),
                                    methodVar);
                            break _emitObjectMethod;
                        }
                    }
                    for (var f : spec.dataFields()) {
                        var descExprOpt = BuiltinMethodEmit.resolveDescriptor(f.fieldJavaType(), leafDescriptors);
                        if (descExprOpt.optIsNone()) continue;
                        var descExpr = descExprOpt.optUnwrap();
                        var rawClassOpt = BuiltinMethodEmit.rawClass(f.fieldJavaType());
                        if (rawClassOpt.optIsNone()) continue;
                        var rawClass = rawClassOpt.optUnwrap();
                        if (spec.isRecord()) {
                            b.addStatement(
                                    "dataFields.add(new $T($S, new $T($S, $T.fromParts($T.fromClass($T.class), $T.of())), $L))",
                                    BuiltinInterfacesGen.OBJECT_DATA_FIELD,
                                    f.langName(),
                                    ClassName.get(
                                            "compilerxyz.main.eval.value.object",
                                            "ObjectDataField",
                                            "Access",
                                            "MethodCall"),
                                    f.jvmFieldName(),
                                    ClassName.get("compilerxyz.main.eval.dynamic", "JavaMethodDescriptor"),
                                    BuiltinInterfacesGen.JAVA_DATA_DESCRIPTOR,
                                    rawClass,
                                    BuiltinInterfacesGen.TLIST,
                                    descExpr);
                        } else {
                            b.addStatement(
                                    "dataFields.add($T.ofField($S, $S, $T.fromClass($T.class), $L))",
                                    BuiltinInterfacesGen.OBJECT_DATA_FIELD,
                                    f.langName(),
                                    f.jvmFieldName(),
                                    BuiltinInterfacesGen.JAVA_DATA_DESCRIPTOR,
                                    rawClass,
                                    descExpr);
                        }
                    }
                    b.addStatement(
                            "instance.linkInit($T.from(dataFields), $T.from(methods))",
                            BuiltinInterfacesGen.ILIST,
                            BuiltinInterfacesGen.ILIST);
                    {
                        buildLink = b.build();
                        break _buildLink;
                    }
                }

                cls.addMethod(buildLink);
                if (spec.isInterface()) {
                    cls.addMethod(MethodSpec.methodBuilder("interfaceData")
                            .addJavadoc(
                                    "The interface contract of this builtin interface, built once from the linked type.\n")
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .returns(BuiltinInterfacesGen.INTERFACE_DATA)
                            .addStatement("return InterfaceHolder.INSTANCE")
                            .build());
                    var ifaceHolder = TypeSpec.classBuilder("InterfaceHolder")
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
                    ifaceHolder.addField(FieldSpec.builder(
                                    BuiltinInterfacesGen.INTERFACE_DATA,
                                    "INSTANCE",
                                    Modifier.PUBLIC,
                                    Modifier.STATIC,
                                    Modifier.FINAL)
                            .initializer("build()")
                            .build());
                    ifaceHolder.addMethod(MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PRIVATE)
                            .build());
                    ifaceHolder.addMethod(MethodSpec.methodBuilder("build")
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .returns(BuiltinInterfacesGen.INTERFACE_DATA)
                            .addStatement("link()")
                            .addStatement(
                                    "return $T.ofBuiltinType(Holder.INSTANCE)", BuiltinInterfacesGen.INTERFACE_DATA)
                            .build());
                    cls.addType(ifaceHolder.build());
                }

                var file = JavaFile.builder(BuiltinInterfacesGen.GEN_PACKAGE, cls.build())
                        .build();
                GenMain.write(outRoot, file, formatter);
            }
        }
        var hostNames = new HashSet<String>();
        var links = new TList<String>();
        for (var s : scan.builtins()) {
            hostNames.add(s.pkg() + "." + s.simpleName());
            links.add("Gen" + s.simpleName());
        }
        {
            var cls = TypeSpec.classBuilder("GenBuiltinInterfaces").addModifiers(Modifier.PUBLIC, Modifier.FINAL);
            cls.addMethod(MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .build());
            var method = MethodSpec.methodBuilder("byIdentity")
                    .addJavadoc("The builtin interface whose identity this is, if any.\n")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(ParameterizedTypeName.get(OPT, BuiltinInterfacesGen.INTERFACE_DATA))
                    .addParameter(BuiltinInterfacesGen.UNIQUE, "identity");
            method.beginControlFlow("if (!(identity instanceof $T b))", BuiltinInterfacesGen.UNIQUE_BUILTIN);
            method.addStatement("return $T.none()", OPT);
            method.endControlFlow();
            method.beginControlFlow("switch ((int) b.id())");
            var ifaceSpecs = new TList<BuiltinInterfacesGen.BuiltinSpec>();
            for (var spec : scan.builtins()) {
                if (spec.isInterface()) ifaceSpecs.add(spec);
            }
            ifaceSpecs.sort((a, c) -> Long.compare(
                    a.isAspect() ? a.identityId() : a.uniqueId(), c.isAspect() ? c.identityId() : c.uniqueId()));
            for (var spec : ifaceSpecs) {
                var identityKey = spec.isAspect() ? spec.identityId() : spec.uniqueId();
                method.addStatement(
                        "case $L: return $T.some($T.interfaceData())",
                        identityKey,
                        OPT,
                        ClassName.get(BuiltinInterfacesGen.GEN_PACKAGE, "Gen" + spec.simpleName()));
            }
            method.addStatement("default: return $T.none()", OPT);
            method.endControlFlow();
            cls.addMethod(method.build());
            var file = JavaFile.builder(BuiltinInterfacesGen.GEN_PACKAGE, cls.build())
                    .build();
            GenMain.write(outRoot, file, formatter);
        }
        TList<BuiltinPathScanner.Entry> scanRoot3;
        _scanRoot:
        {
            var out = new TList<BuiltinPathScanner.Entry>();
            try (Stream<Path> stream = Files.walk(runtimeSourceRoot)) {
                var files = stream.filter(p -> p.toString().endsWith(".java")).toList();
                for (var file : files) {
                    var src = Files.readString(file);
                    if (!src.contains("BuiltinPath")) continue;
                    var cu = StaticJavaParser.parse(file);
                    var pkg = cu.getPackageDeclaration()
                            .map(pd -> pd.getNameAsString())
                            .orElse("");
                    TMap<String, String> buildImportMap;
                    _buildImportMap:
                    {
                        var m = new TMap<String, String>();
                        for (var imp : cu.getImports()) {
                            var full = imp.getNameAsString();
                            var simple = full.substring(full.lastIndexOf('.') + 1);
                            m.put(simple, full);
                        }
                        {
                            buildImportMap = m;
                            break _buildImportMap;
                        }
                    }
                    TMap<String, String> imports = buildImportMap;

                    for (var t : cu.getTypes()) {
                        BuiltinPathScanner.walkType(pkg, pkg, "", t, imports, out, scannedLeaves);
                    }
                }
            }
            {
                scanRoot3 = out;
                break _scanRoot;
            }
        }
        TList<BuiltinPathScanner.Entry> pathEntries = scanRoot3;

        TList<BuiltinIdentityScanner.IdentitySpec> scanRoot4;

        _scanRoot:
        {
            var out = new TList<BuiltinIdentityScanner.IdentitySpec>();
            try (Stream<Path> stream = Files.walk(runtimeSourceRoot)) {
                var files = stream.filter(p -> p.toString().endsWith(".java")).toList();
                for (var file : files) {
                    var src = Files.readString(file);
                    if (!src.contains("@BuiltinFull(") && !src.contains("@BuiltinLight(")) continue;
                    var cu = StaticJavaParser.parse(file);
                    for (var t : cu.getTypes()) {
                        BuiltinIdentityScanner.collect(t, "", out);
                    }
                }
            }
            {
                scanRoot4 = out;
                break _scanRoot;
            }
        }

        TList<BuiltinIdentityScanner.IdentitySpec> scannedIdentities = scanRoot4;

        var identityExtras = new TList<IdentitySpec>();
        for (var s : scannedIdentities) {
            identityExtras.add(new IdentitySpec(s.name(), s.value(), s.standalone()));
        }
        TList<IdentitySpec> identitySpecs;
        _identitySpecs:
        {
            var out = new TList<IdentitySpec>();
            for (var prim : ListsGen.PRIMS) {
                out.add(new IdentitySpec(ListsGen.snake(ListsGen.typeSimple(prim)), prim.uniqueId(), true));
            }
            for (var prim : OptionsGen.PRIMS) {
                out.add(new IdentitySpec(ListsGen.snake(OptionsGen.typeSimple(prim)), prim.uniqueId(), true));
            }
            {
                identitySpecs = out;
                break _identitySpecs;
            }
        }
        identityExtras.addAll(identitySpecs);
        {
            TList<IdentitySpec> extras = identityExtras;

            var specs = new TList<IdentitySpec>();
            specs.addAll(extras);
            var seenNames = new TSet<String>();
            for (var spec : specs) {
                if (!seenNames.add(spec.name())) {
                    throw new IllegalStateException("duplicate identity name: " + spec.name());
                }
                if (spec.standalone() && !aspectIds.add(spec.builtinUniqueId())) {
                    throw new IllegalStateException(
                            "duplicate aspect id " + spec.builtinUniqueId() + " (in identities)");
                }
            }

            var cls = TypeSpec.classBuilder("GenTrivialIdentityAspects").addModifiers(Modifier.PUBLIC, Modifier.FINAL);

            cls.addMethod(MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .build());

            for (var spec : specs) {
                String snake = spec.name();
                String constantName;
                _constantName:
                {
                    var s = snake;
                    if (s.endsWith("_identity")) s = s.substring(0, s.length() - "_identity".length());
                    {
                        constantName = s.toUpperCase(Locale.ROOT) + "_IDENTITY";
                        break _constantName;
                    }
                }
                cls.addField(FieldSpec.builder(UNIQUE, constantName, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T($LL)", UNIQUE_BUILTIN, spec.builtinUniqueId())
                        .build());
            }

            var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
            GenMain.write(outRoot, file, formatter);
        }
        {
            for (var store : ListsGen.STORES) {
                for (var bytecodeCalled : new String[] {"push", "get", "size"}) {
                    dispatchRoots.add(ListsGen.GEN_PACKAGE + "." + store.simple() + "#" + bytecodeCalled);
                }
                TypeSpec storeClass;
                _storeClass:
                {
                    var arrayType = ArrayTypeName.of(store.javaPrim());
                    {
                        storeClass = TypeSpec.classBuilder(store.simple())
                                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                .addSuperinterface(ClassName.get("compilerxyz.main.eval.value.list", "ListStore"))
                                .addField(FieldSpec.builder(
                                                arrayType, "EMPTY", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                        .initializer("new $T[0]", store.javaPrim())
                                        .build())
                                .addField(FieldSpec.builder(arrayType, "data", Modifier.PRIVATE)
                                        .initializer("EMPTY")
                                        .build())
                                .addField(FieldSpec.builder(TypeName.INT, "size", Modifier.PRIVATE)
                                        .build())
                                .addMethod(MethodSpec.methodBuilder("push")
                                        .addModifiers(Modifier.PUBLIC)
                                        .addParameter(store.javaPrim(), "value")
                                        .beginControlFlow("if (size == data.length)")
                                        .addStatement(
                                                "data = $T.copyOf(data, $T.max(8, data.length * 2))",
                                                Arrays.class,
                                                Math.class)
                                        .endControlFlow()
                                        .addStatement("data[size] = value")
                                        .addStatement("size += 1")
                                        .build())
                                .addMethod(MethodSpec.methodBuilder("get")
                                        .addModifiers(Modifier.PUBLIC)
                                        .returns(store.javaPrim())
                                        .addParameter(TypeName.INT, "index")
                                        .beginControlFlow("if (index >= size)")
                                        .addStatement("throw new $T(index)", ArrayIndexOutOfBoundsException.class)
                                        .endControlFlow()
                                        .addStatement("return data[index]")
                                        .build())
                                .addMethod(MethodSpec.methodBuilder("size")
                                        .addModifiers(Modifier.PUBLIC)
                                        .returns(TypeName.INT)
                                        .addStatement("return size")
                                        .build())
                                .addMethod(MethodSpec.methodBuilder("listStoreSize")
                                        .addAnnotation(Override.class)
                                        .addModifiers(Modifier.PUBLIC)
                                        .returns(TypeName.INT)
                                        .addStatement("return size")
                                        .build())
                                .addMethod(MethodSpec.methodBuilder("listStoreBoxed")
                                        .addAnnotation(Override.class)
                                        .addModifiers(Modifier.PUBLIC)
                                        .returns(Object.class)
                                        .addParameter(TypeName.INT, "index")
                                        .addStatement("return get(index)")
                                        .build())
                                .build();
                        break _storeClass;
                    }
                }
                GenMain.write(
                        outRoot,
                        JavaFile.builder(ListsGen.GEN_PACKAGE, storeClass).build(),
                        formatter);
            }
            TypeSpec factoryClass;
            _factoryClass:
            {
                var method = MethodSpec.methodBuilder("listDynamic")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ListsGen.LIST_DYNAMIC)
                        .addParameter(ListsGen.EXPR_CONTEXT, "xx")
                        .addParameter(ListsGen.TYPE, "elementType");
                for (var prim : ListsGen.PRIMS) {
                    method.beginControlFlow(
                            "if (elementType.typeAspect(xx, $T.$L).optIsSome())",
                            ListsGen.GEN_IDENTITIES,
                            ListsGen.constantName(
                                    ListsGen.snake(prim.elementClass().simpleName())));
                    method.addStatement(
                            "return new $T(new $T(), $T.fromClass($T.class), $T.$L)",
                            ListsGen.LIST_SPECIALIZED,
                            prim.elementClass(),
                            ListsGen.JAVA_INTERNAL_NAME,
                            ClassName.get(ListsGen.GEN_PACKAGE, prim.storeSimple()),
                            ListsGen.GEN_IDENTITIES,
                            ListsGen.constantName(ListsGen.snake(ListsGen.typeSimple(prim))));
                    method.endControlFlow();
                }
                method.addStatement("return new $T(elementType)", ListsGen.LIST_BOXED);
                {
                    factoryClass = TypeSpec.classBuilder("GenLists")
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .addMethod(MethodSpec.constructorBuilder()
                                    .addModifiers(Modifier.PRIVATE)
                                    .build())
                            .addMethod(method.build())
                            .build();
                    break _factoryClass;
                }
            }
            GenMain.write(
                    outRoot,
                    JavaFile.builder(ListsGen.GEN_PACKAGE, factoryClass).build(),
                    formatter);
        }
        {
            for (var store : OptionsGen.STORES) {
                var self = ClassName.get(ListsGen.GEN_PACKAGE, store.simple());
                var boxed = store.javaPrim().box();
                var optOfBoxed = ParameterizedTypeName.get(OptionsGen.OPT, boxed);
                var storeClass = TypeSpec.classBuilder(store.simple())
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addSuperinterface(ClassName.get("compilerxyz.main.eval.value.option", "OptionStore"))
                        .addField(FieldSpec.builder(self, "NONE", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                .initializer("new $T(false, $L)", self, store.zeroLiteral())
                                .build())
                        .addField(FieldSpec.builder(TypeName.BOOLEAN, "present", Modifier.PRIVATE, Modifier.FINAL)
                                .build())
                        .addField(FieldSpec.builder(store.javaPrim(), "value", Modifier.PRIVATE, Modifier.FINAL)
                                .build())
                        .addMethod(MethodSpec.constructorBuilder()
                                .addModifiers(Modifier.PRIVATE)
                                .addParameter(TypeName.BOOLEAN, "present")
                                .addParameter(store.javaPrim(), "value")
                                .addStatement("this.present = present")
                                .addStatement("this.value = value")
                                .build())
                        .addMethod(MethodSpec.methodBuilder("some")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(self)
                                .addParameter(store.javaPrim(), "value")
                                .addStatement("return new $T(true, value)", self)
                                .build())
                        .addMethod(MethodSpec.methodBuilder("none")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(self)
                                .addStatement("return NONE")
                                .build())
                        .addMethod(MethodSpec.methodBuilder("isSome")
                                .addModifiers(Modifier.PUBLIC)
                                .returns(TypeName.BOOLEAN)
                                .addStatement("return present")
                                .build())
                        .addMethod(MethodSpec.methodBuilder("value")
                                .addModifiers(Modifier.PUBLIC)
                                .returns(store.javaPrim())
                                .beginControlFlow("if (!present)")
                                .addStatement("throw new $T($S)", IllegalStateException.class, "option value of none")
                                .endControlFlow()
                                .addStatement("return value")
                                .build())
                        .addMethod(MethodSpec.methodBuilder("fromOpt")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(self)
                                .addParameter(optOfBoxed, "opt")
                                .addStatement("return opt.optIsSome() ? some(opt.optUnwrap()) : NONE")
                                .build())
                        .addMethod(MethodSpec.methodBuilder("toOpt")
                                .addModifiers(Modifier.PUBLIC)
                                .returns(optOfBoxed)
                                .addStatement(
                                        "return present ? $T.some(value) : $T.none()", OptionsGen.OPT, OptionsGen.OPT)
                                .build())
                        .addMethod(MethodSpec.methodBuilder("optionStoreBoxed")
                                .addAnnotation(Override.class)
                                .addModifiers(Modifier.PUBLIC)
                                .returns(ParameterizedTypeName.get(OptionsGen.OPT, ClassName.get(Object.class)))
                                .addStatement(
                                        "return present ? $T.some(value) : $T.none()", OptionsGen.OPT, OptionsGen.OPT)
                                .build())
                        .build();
                GenMain.write(
                        outRoot,
                        JavaFile.builder(ListsGen.GEN_PACKAGE, storeClass).build(),
                        formatter);
            }
            var method = MethodSpec.methodBuilder("optionDynamic")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(OptionsGen.OPTION_DYNAMIC)
                    .addParameter(ListsGen.EXPR_CONTEXT, "xx")
                    .addParameter(ListsGen.TYPE, "elementType");
            for (var prim : OptionsGen.PRIMS) {
                method.beginControlFlow(
                        "if (elementType.typeAspect(xx, $T.$L).optIsSome())",
                        ListsGen.GEN_IDENTITIES,
                        ListsGen.constantName(ListsGen.snake(prim.elementClass().simpleName())));
                method.addStatement(
                        "return new $T(new $T(), $T.fromClass($T.class), $T.$L)",
                        OptionsGen.OPTION_SPECIALIZED,
                        prim.elementClass(),
                        ListsGen.JAVA_INTERNAL_NAME,
                        ClassName.get(ListsGen.GEN_PACKAGE, prim.storeSimple()),
                        ListsGen.GEN_IDENTITIES,
                        ListsGen.constantName(ListsGen.snake(OptionsGen.typeSimple(prim))));
                method.endControlFlow();
            }
            method.addStatement("return new $T(elementType)", OptionsGen.OPTION_BOXED);
            var factoryClass = TypeSpec.classBuilder("GenOptions")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addMethod(MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PRIVATE)
                            .build())
                    .addMethod(method.build())
                    .build();
            GenMain.write(
                    outRoot,
                    JavaFile.builder(ListsGen.GEN_PACKAGE, factoryClass).build(),
                    formatter);
        }

        {
            TList<String> linkClassNames = links;

            TList<BuiltinPathScanner.Entry> scannedPathEntries = pathEntries;

            BuiltinTreeGen.NodeSpec canonicalSpec;
            _canonicalSpec:
            {
                var pathToLeaf = new LinkedHashMap<String, BuiltinTreeGen.LeafSpec>();
                for (var entry : scannedPathEntries) {
                    BuiltinTreeGen.LeafSpec spec =
                            switch (entry) {
                                case BuiltinPathScanner.TypeLeaf tl -> {
                                    var wrapper = BuiltinTreeGen.TYPE_OF_TYPES_EXPR;
                                    yield BuiltinTreeGen.LeafSpec.of(
                                            DynamicTypes.leaf(tl.typeClass(), scannedLeaves), wrapper);
                                }
                                case BuiltinPathScanner.FunctionLeaf fl -> {
                                    Opt<CodeBlock> retDynamicType = fl.retType().optIsSome()
                                            ? Opt.some(fl.retType().optUnwrap().dynamicType())
                                            : Opt.none();
                                    var retExpr = retDynamicType.optIsSome()
                                            ? CodeBlock.of(
                                                    "$T.some($L)", BuiltinTreeGen.OPT, retDynamicType.optUnwrap())
                                            : CodeBlock.of("$T.<$T>none()", BuiltinTreeGen.OPT, BuiltinTreeGen.TYPE);
                                    CodeBlock rParamsExpr;
                                    _paramsExpr:
                                    {
                                        var functionParam =
                                                ClassName.get("compilerxyz.main.eval.value.function", "FunctionParam");
                                        var b = CodeBlock.builder();
                                        b.add("$T.<$T>of(", BuiltinTreeGen.TLIST, functionParam);
                                        for (var i = 0; i < fl.args().size(); i++) {
                                            var a = fl.args().get(i);
                                            if (i > 0) b.add(", ");
                                            b.add(
                                                    "new $T($S, $L)",
                                                    functionParam,
                                                    a.name(),
                                                    a.type().dynamicType());
                                        }
                                        b.add(")");
                                        {
                                            rParamsExpr = b.build();
                                            break _paramsExpr;
                                        }
                                    }
                                    dispatchRoots.add(fl.ownerInternalName().replace('/', '.') + "#" + fl.methodName());
                                    var innerExpr = CodeBlock.of(
                                            "new $T($L, $L, $T.of($S), $S, $L)",
                                            BuiltinTreeGen.TYPE_FUNCTION_DYNAMIC,
                                            rParamsExpr,
                                            retExpr,
                                            BuiltinTreeGen.JAVA_INTERNAL_NAME,
                                            fl.ownerInternalName(),
                                            fl.methodName(),
                                            fl.needsEvalContext());
                                    yield new BuiltinTreeGen.LeafSpec(innerExpr, BuiltinTreeGen.TYPE_OF_TYPES_EXPR);
                                }
                            };
                    if (pathToLeaf.put(entry.entryPath(), spec) != null) {
                        throw new IllegalStateException("duplicate @BuiltinPath entry: " + entry.entryPath());
                    }
                }
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_initial_bind_dynamic",
                        CodeBlock.of(
                                "$T.dynamicType()",
                                ClassName.get("compilerxyz.main.gen", "GenAspectInitialBindDynamic")),
                        BuiltinTreeGen.TYPE_OF_TYPES_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_lift",
                        CodeBlock.of("$T.dynamicType()", ClassName.get("compilerxyz.main.gen", "GenAspectLift")),
                        BuiltinTreeGen.TYPE_OF_TYPES_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_bind_dynamic",
                        CodeBlock.of("$T.dynamicType()", ClassName.get("compilerxyz.main.gen", "GenAspectBindDynamic")),
                        BuiltinTreeGen.TYPE_OF_TYPES_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_consume_dynamic",
                        CodeBlock.of(
                                "$T.dynamicType()", ClassName.get("compilerxyz.main.gen", "GenAspectConsumeDynamic")),
                        BuiltinTreeGen.TYPE_OF_TYPES_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_string_dynamic",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectStringDynamic")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_function_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectFunction")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_field_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectField")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_bind_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectBind")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_drop_dynamic_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectDropDynamic")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_consume_dynamic_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectConsumeDynamic")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_bind_dynamic_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectBindDynamic")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_set_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectSet")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_operator_add_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectOperatorAdd")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_operator_lt_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectOperatorLt")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_operator_sub_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectOperatorSub")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_operator_mul_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectOperatorMul")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_operator_div_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectOperatorDiv")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_operator_mod_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectOperatorMod")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_operator_and_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectOperatorAnd")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_operator_or_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectOperatorOr")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_operator_xor_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectOperatorXor")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_operator_shl_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectOperatorShl")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_operator_shr_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectOperatorShr")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_operator_ushr_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectOperatorUshr")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_operator_eq_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectOperatorEq")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_operator_ne_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectOperatorNe")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_operator_gt_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectOperatorGt")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_operator_le_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectOperatorLe")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_operator_ge_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectOperatorGe")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_operator_neg_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectOperatorNeg")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_operator_bit_not_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectOperatorBitNot")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_iterable_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectIterable")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_tuple_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectTuple")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_artifact_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectArtifact")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_record_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectRecord")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_type_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectType")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_lift_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectLift")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_field_use_dynamic_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectFieldUseDynamic")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_initial_bind_dynamic_id",
                        CodeBlock.of(
                                "$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectInitialBindDynamic")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.type_int_dynamic_identity",
                        CodeBlock.of(
                                "$T.TYPE_INT_DYNAMIC_IDENTITY",
                                ClassName.get("compilerxyz.main.gen", "GenTrivialIdentityAspects")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.type_long_dynamic_identity",
                        CodeBlock.of(
                                "$T.TYPE_LONG_DYNAMIC_IDENTITY",
                                ClassName.get("compilerxyz.main.gen", "GenTrivialIdentityAspects")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);

                var root = new BuiltinTreeGen.TreeNode();
                for (var e : pathToLeaf.entrySet()) {
                    var parts = e.getKey().split("\\.");
                    var cur = root;
                    for (int i = 0; i < parts.length - 1; i++) {
                        var name = parts[i];
                        var next = cur.children().get(name);
                        if (next == null) {
                            next = new BuiltinTreeGen.TreeNode();
                            cur.children().put(name, next);
                        }
                        if (next.leaf().optIsSome()) {
                            throw new IllegalStateException("path collision (leaf where branch needed): " + e.getKey());
                        }
                        cur = next;
                    }
                    var leafName = parts[parts.length - 1];
                    if (cur.children().containsKey(leafName)) {
                        throw new IllegalStateException("path collision: " + e.getKey());
                    }
                    cur.children().put(leafName, new BuiltinTreeGen.TreeNode(e.getValue()));
                }
                {
                    canonicalSpec = BuiltinTreeGen.toNodeSpec(root, "");
                    break _canonicalSpec;
                }
            }
            BuiltinTreeGen.NodeSpec spec = canonicalSpec;

            BuiltinTreeGen.NodeSpec root = spec;

            for (var id : new long[] {
                BuiltinTreeGen.ASPECT_COMPILE_SCOPE_ID,
                BuiltinTreeGen.THROW_COMPILE_SCOPE_ID,
                BuiltinTreeGen.PANIC_COMPILE_SCOPE_ID
            }) {
                if (!scopeIds.add(id)) {
                    throw new IllegalStateException(
                            "duplicate scope_id " + id + " (in builtin_tree, compile-scope marker)");
                }
            }
            var seenNames = new TSet<String>();
            BuiltinTreeGen.collectAndValidate(root, "", seenNames, scopeIds);

            var cls = TypeSpec.classBuilder("GenBuiltinTree").addModifiers(Modifier.PUBLIC, Modifier.FINAL);

            cls.addMethod(MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .build());

            MethodSpec installRootFactory;

            _installRootFactory:
            {
                var m = MethodSpec.methodBuilder("installRoot")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ParameterizedTypeName.get(BuiltinTreeGen.ERESULT, BuiltinTreeGen.BINDING))
                        .addParameter(ParameterSpec.builder(BuiltinTreeGen.EXPR_CONTEXT, "xx")
                                .build())
                        .addParameter(ParameterSpec.builder(BuiltinTreeGen.QEXPR_ID, "loc")
                                .build())
                        .addParameter(ParameterSpec.builder(BuiltinTreeGen.IMPORT_STATE_ID, "importState")
                                .build());

                for (var linkName : linkClassNames) {
                    m.addStatement("$T.link()", ClassName.get(BuiltinTreeGen.GEN_PACKAGE, linkName));
                }
                var st = new BuiltinTreeGen.EmitState();
                var vars = BuiltinTreeGen.emitNode(root, st);
                m.addCode(st.body.build());
                m.addStatement("return $T.ok($N)", BuiltinTreeGen.ERESULT, vars[0]);
                {
                    installRootFactory = m.build();
                    break _installRootFactory;
                }
            }

            cls.addMethod(installRootFactory);

            var file = JavaFile.builder(BuiltinTreeGen.GEN_PACKAGE, cls.build()).build();
            GenMain.write(outRoot, file, formatter);
        }
        {
            StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
            var index = new CodecsGen.TypeIndex();
            index.scan(runtimeSourceRoot);

            var roots = new TList<String>();
            for (var entry : index.byQname.entrySet()) {
                if (entry.getValue().implementsMarker) {
                    roots.add(entry.getKey());
                }
            }
            var sortedRoots = TList.from(roots);
            sortedRoots.sort(String::compareTo);

            var closure = new TOrderedSet<String>();
            for (var root : sortedRoots) {
                CodecsGen.walk(root, index, closure);
            }
            var streamWalk = new TOrderedSet<String>();
            for (var root : sortedRoots) {
                if (index.byQname.getOpt(root).optUnwrap().implementsStreamMarker) {
                    CodecsGen.walk(root, index, streamWalk);
                }
            }
            var streamClosure = new TSet<String>();
            for (var qname : streamWalk) streamClosure.add(qname);

            for (var qname : closure) {
                if (CodecsGen.HAND_CODED.contains(qname)) continue;
                var tOpt = index.byQname.getOpt(qname);
                if (tOpt.optIsNone()) continue;
                var t = tOpt.optUnwrap();
                if (t.kind == TypeKind.RECORD || t.kind == TypeKind.SEALED || t.kind == TypeKind.ENUM) {
                    CodecsGen.TypeInfo info = t;
                    String emit;
                    _emit:
                    {
                        String ctxQname = qname;

                        for (var c : info.components) {
                            CodecEmitter.resolveType(c.type, ctxQname, index);
                        }
                        var resolvedPerms = new TList<String>();
                        for (var perm : info.permittedSubclasses) {

                            if (perm.contains(".") && index.byQname.containsKey(perm)) {
                                resolvedPerms.add(perm);
                            } else {
                                var resolved = index.resolve(perm, ctxQname);
                                resolvedPerms.add(resolved.optUnwrapOr(perm));
                            }
                        }
                        info.permittedSubclasses.clear();
                        info.permittedSubclasses.addAll(resolvedPerms);

                        var simpleName = CodecEmitter.simpleNameOf(qname);
                        var codecClassName = simpleName + "Codec";
                        TypeName classNameOf;
                        _classNameOf:
                        {
                            var lastDot = qname.lastIndexOf('.');
                            if (lastDot < 0) {
                                {
                                    classNameOf = ClassName.bestGuess(qname);
                                    break _classNameOf;
                                }
                            }
                            var pkg = qname.substring(0, lastDot);
                            var rest = qname.substring(lastDot + 1);
                            var parts = rest.split("\\.");
                            {
                                classNameOf = ClassName.get(pkg, parts[0], Arrays.copyOfRange(parts, 1, parts.length));
                                break _classNameOf;
                            }
                        }
                        TypeName target = classNameOf;

                        var cls = TypeSpec.classBuilder(codecClassName).addModifiers(Modifier.PUBLIC, Modifier.FINAL);

                        cls.addMethod(MethodSpec.constructorBuilder()
                                .addModifiers(Modifier.PRIVATE)
                                .build());

                        if (streamClosure.contains(qname)) {
                            if (info.kind == TypeKind.RECORD) {
                                MethodSpec buildRecordEncode;
                                _buildRecordEncode:
                                {
                                    var body = CodeBlock.builder();
                                    body.addStatement("writer.beginObject()");
                                    body.beginControlFlow("if (style == $T.CANONICAL)", CodecEmitter.JSON_STYLE);
                                    TList<Component> cs = info.components;
                                    TList<Component> sortedBySnake;
                                    _sortedBySnake:
                                    {
                                        var out = TList.from(cs);
                                        out.sort((a, b) -> CodecEmitter.toSnakeCase(a.name)
                                                .compareTo(CodecEmitter.toSnakeCase(b.name)));
                                        {
                                            sortedBySnake = out;
                                            break _sortedBySnake;
                                        }
                                    }
                                    for (var c : sortedBySnake) {
                                        body.add(CodecEmitter.encodeFieldBlock(c));
                                    }
                                    body.nextControlFlow("else");
                                    for (var c : info.components) {
                                        body.add(CodecEmitter.encodeFieldBlock(c));
                                    }
                                    body.endControlFlow();
                                    body.addStatement("writer.endObject()");
                                    {
                                        buildRecordEncode = MethodSpec.methodBuilder("encode")
                                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                .returns(void.class)
                                                .addParameter(CodecEmitter.JSON_WRITER, "writer")
                                                .addParameter(target, "value")
                                                .addParameter(CodecEmitter.JSON_STYLE, "style")
                                                .addException(CodecEmitter.IO_EXCEPTION)
                                                .addCode(body.build())
                                                .build();
                                        break _buildRecordEncode;
                                    }
                                }
                                cls.addMethod(buildRecordEncode);
                                MethodSpec buildRecordDecode;
                                _buildRecordDecode:
                                {
                                    var body = CodeBlock.builder();
                                    for (var c : info.components) {
                                        var boxed = c.type.kind != CodecsGen.ParsedTypeKind.PRIMITIVE
                                                ? CodecEmitter.javaTypeOf(c.type)
                                                : switch (c.type.name) {
                                                    case "boolean" -> "Boolean";
                                                    case "char" -> "Character";
                                                    case "float" -> "Float";
                                                    case "double" -> "Double";
                                                    case "long" -> "Long";
                                                    case "int" -> "Integer";
                                                    case "short" -> "Short";
                                                    case "byte" -> "Byte";
                                                    default ->
                                                        throw new IllegalStateException(
                                                                "Unsupported primitive: " + c.type.name);
                                                };
                                        body.addStatement(
                                                "$T<$L> __$L = $T.none()",
                                                CodecEmitter.OPT,
                                                boxed,
                                                c.name,
                                                CodecEmitter.OPT);
                                    }
                                    body.addStatement("reader.beginObject()");
                                    body.beginControlFlow("while (reader.hasNext())");
                                    body.addStatement("String __fname = reader.nextName()");
                                    body.beginControlFlow("switch (__fname)");
                                    for (var c : info.components) {
                                        var key = CodecEmitter.toSnakeCase(c.name);
                                        body.beginControlFlow("case $S ->", key);
                                        body.addStatement("$T __p = path.field($S)", CodecEmitter.FIELD_PATH, key);
                                        body.addStatement("$L __v", CodecEmitter.javaTypeOf(c.type));
                                        body.add(
                                                CodecEmitter.decodeStmts(c.type, "reader", CodeBlock.of("__p"), "__v"));
                                        body.addStatement("__$L = $T.some(__v)", c.name, CodecEmitter.OPT);
                                        body.endControlFlow();
                                    }
                                    body.add(
                                            "default -> throw new $T($S + __fname + $S, path.field(__fname));\n",
                                            CodecEmitter.DESERIALIZATION_EXCEPTION,
                                            "Unknown field '",
                                            "' in " + qname);
                                    body.endControlFlow();
                                    body.endControlFlow();
                                    body.addStatement("reader.endObject()");
                                    for (var c : info.components) {
                                        var key = CodecEmitter.toSnakeCase(c.name);
                                        body.addStatement(
                                                "if (__$L.optIsNone()) throw new $T($S, path.field($S))",
                                                c.name,
                                                CodecEmitter.DESERIALIZATION_EXCEPTION,
                                                "Missing required field '" + c.name + "'",
                                                key);
                                    }
                                    var ctorArgs = CodeBlock.builder();
                                    for (var i = 0; i < info.components.size(); i++) {
                                        if (i > 0) ctorArgs.add(", ");
                                        ctorArgs.add("__$L.optUnwrap()", info.components.get(i).name);
                                    }
                                    body.addStatement("return new $L($L)", qname, ctorArgs.build());
                                    {
                                        buildRecordDecode = MethodSpec.methodBuilder("decode")
                                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                .returns(target)
                                                .addParameter(CodecEmitter.JSON_READER, "reader")
                                                .addParameter(CodecEmitter.FIELD_PATH, "path")
                                                .addException(CodecEmitter.IO_EXCEPTION)
                                                .addCode(body.build())
                                                .build();
                                        break _buildRecordDecode;
                                    }
                                }
                                cls.addMethod(buildRecordDecode);
                            } else if (info.kind == TypeKind.SEALED) {
                                MethodSpec buildSealedEncode;
                                _buildSealedEncode:
                                {
                                    var body = CodeBlock.builder();
                                    body.addStatement("writer.beginObject()");
                                    for (var perm : info.permittedSubclasses) {
                                        var key = CodecEmitter.toSnakeCase(CodecEmitter.lastSegment(perm));
                                        body.beginControlFlow("if (value instanceof $L __v)", perm);
                                        body.addStatement("writer.name($S)", key);
                                        body.addStatement(
                                                "$L.encode(writer, __v, style)",
                                                CodecEmitter.codecTargetFor(perm)
                                                        .klass());
                                        body.nextControlFlow("else");
                                    }
                                    body.addStatement(
                                            "throw new IllegalStateException($S + value.getClass())",
                                            "Unhandled variant of " + qname + ": ");
                                    for (var i = 0; i < info.permittedSubclasses.size(); i++) {
                                        body.endControlFlow();
                                    }
                                    body.addStatement("writer.endObject()");
                                    {
                                        buildSealedEncode = MethodSpec.methodBuilder("encode")
                                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                .returns(void.class)
                                                .addParameter(CodecEmitter.JSON_WRITER, "writer")
                                                .addParameter(target, "value")
                                                .addParameter(CodecEmitter.JSON_STYLE, "style")
                                                .addException(CodecEmitter.IO_EXCEPTION)
                                                .addCode(body.build())
                                                .build();
                                        break _buildSealedEncode;
                                    }
                                }
                                cls.addMethod(buildSealedEncode);
                                MethodSpec buildSealedDecode;
                                _buildSealedDecode:
                                {
                                    var body = CodeBlock.builder();
                                    body.addStatement("reader.beginObject()");
                                    body.addStatement(
                                            "if (!reader.hasNext()) throw new $T($S, path)",
                                            CodecEmitter.DESERIALIZATION_EXCEPTION,
                                            "Empty object for sealed " + qname);
                                    body.addStatement("String __tag = reader.nextName()");
                                    body.addStatement("$L __out", qname);
                                    body.beginControlFlow("switch (__tag)");
                                    for (var perm : info.permittedSubclasses) {
                                        var key = CodecEmitter.toSnakeCase(CodecEmitter.lastSegment(perm));
                                        body.add(
                                                "case $S -> __out = $L.decode(reader, path.field($S));\n",
                                                key,
                                                CodecEmitter.codecTargetFor(perm)
                                                        .klass(),
                                                key);
                                    }
                                    body.add(
                                            "default -> throw new $T($S + __tag + $S, path);\n",
                                            CodecEmitter.DESERIALIZATION_EXCEPTION,
                                            "Unknown variant '",
                                            "' for " + qname);
                                    body.endControlFlow();
                                    body.addStatement(
                                            "if (reader.hasNext()) throw new $T($S, path)",
                                            CodecEmitter.DESERIALIZATION_EXCEPTION,
                                            "Sealed " + qname + " must have exactly one variant");
                                    body.addStatement("reader.endObject()");
                                    body.addStatement("return __out");
                                    {
                                        buildSealedDecode = MethodSpec.methodBuilder("decode")
                                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                .returns(target)
                                                .addParameter(CodecEmitter.JSON_READER, "reader")
                                                .addParameter(CodecEmitter.FIELD_PATH, "path")
                                                .addException(CodecEmitter.IO_EXCEPTION)
                                                .addCode(body.build())
                                                .build();
                                        break _buildSealedDecode;
                                    }
                                }
                                cls.addMethod(buildSealedDecode);
                            } else if (info.kind == TypeKind.ENUM) {
                                MethodSpec buildEnumEncode;
                                _buildEnumEncode:
                                {
                                    {
                                        buildEnumEncode = MethodSpec.methodBuilder("encode")
                                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                .returns(void.class)
                                                .addParameter(CodecEmitter.JSON_WRITER, "writer")
                                                .addParameter(target, "value")
                                                .addParameter(CodecEmitter.JSON_STYLE, "style")
                                                .addException(CodecEmitter.IO_EXCEPTION)
                                                .addStatement(
                                                        "writer.value(value.name().toLowerCase(java.util.Locale.ROOT))")
                                                .build();
                                        break _buildEnumEncode;
                                    }
                                }
                                cls.addMethod(buildEnumEncode);
                                MethodSpec buildEnumDecode;
                                _buildEnumDecode:
                                {
                                    var body = CodeBlock.builder();
                                    body.addStatement("String __s = reader.nextString()");
                                    body.addStatement("String __u = __s.toUpperCase(java.util.Locale.ROOT)");
                                    body.beginControlFlow("for ($L __c : $L.values())", qname, qname);
                                    body.addStatement("if (__c.name().equals(__u)) return __c");
                                    body.endControlFlow();
                                    body.addStatement(
                                            "throw new $T($S + __s + $S, path)",
                                            CodecEmitter.DESERIALIZATION_EXCEPTION,
                                            "Unknown enum constant '",
                                            "' for " + qname);
                                    {
                                        buildEnumDecode = MethodSpec.methodBuilder("decode")
                                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                .returns(target)
                                                .addParameter(CodecEmitter.JSON_READER, "reader")
                                                .addParameter(CodecEmitter.FIELD_PATH, "path")
                                                .addException(CodecEmitter.IO_EXCEPTION)
                                                .addCode(body.build())
                                                .build();
                                        break _buildEnumDecode;
                                    }
                                }
                                cls.addMethod(buildEnumDecode);
                            } else {
                                throw new IllegalStateException("Unsupported kind: " + info.kind + " for " + qname);
                            }
                        }

                        {
                            var toBody = CodeBlock.builder();
                            var fromBody = CodeBlock.builder();
                            if (info.kind == TypeKind.RECORD) {
                                toBody.addStatement(
                                        "$T<String, $T> __o = new $T<>()",
                                        CodecEmitter.TORDERED_MAP,
                                        CodecEmitter.JSON_VALUE,
                                        CodecEmitter.TORDERED_MAP);
                                var fi = 0;
                                for (var c : info.components) {
                                    var key = CodecEmitter.toSnakeCase(c.name);
                                    var fv = "__f" + fi++;
                                    toBody.add(
                                            CodecEmitter.toValueStmts(c.type, CodeBlock.of("value.$L()", c.name), fv));
                                    toBody.addStatement("__o.put($S, $L)", key, fv);
                                }
                                toBody.addStatement("return new $T(__o)", CodecEmitter.JSON_VALUE_OBJECT);

                                for (var c : info.components) {
                                    var boxed = c.type.kind != CodecsGen.ParsedTypeKind.PRIMITIVE
                                            ? CodecEmitter.javaTypeOf(c.type)
                                            : switch (c.type.name) {
                                                case "boolean" -> "Boolean";
                                                case "char" -> "Character";
                                                case "float" -> "Float";
                                                case "double" -> "Double";
                                                case "long" -> "Long";
                                                case "int" -> "Integer";
                                                case "short" -> "Short";
                                                case "byte" -> "Byte";
                                                default ->
                                                    throw new IllegalStateException(
                                                            "Unsupported primitive: " + c.type.name);
                                            };
                                    fromBody.addStatement(
                                            "$T<$L> __$L = $T.none()",
                                            CodecEmitter.OPT,
                                            boxed,
                                            c.name,
                                            CodecEmitter.OPT);
                                }
                                fromBody.beginControlFlow(
                                        "for (var __entry : $T.object(value, path).entrySet())",
                                        CodecEmitter.JSON_VALUES);
                                fromBody.addStatement("String __fname = __entry.getKey()");
                                fromBody.beginControlFlow("switch (__fname)");
                                for (var c : info.components) {
                                    var key = CodecEmitter.toSnakeCase(c.name);
                                    fromBody.beginControlFlow("case $S ->", key);
                                    fromBody.addStatement("$T __p = path.field($S)", CodecEmitter.FIELD_PATH, key);
                                    fromBody.addStatement("$L __v", CodecEmitter.javaTypeOf(c.type));
                                    fromBody.add(CodecEmitter.fromValueStmts(
                                            c.type, CodeBlock.of("__entry.getValue()"), CodeBlock.of("__p"), "__v"));
                                    fromBody.addStatement("__$L = $T.some(__v)", c.name, CodecEmitter.OPT);
                                    fromBody.endControlFlow();
                                }
                                fromBody.add(
                                        "default -> throw new $T($S + __fname + $S, path.field(__fname));\n",
                                        CodecEmitter.DESERIALIZATION_EXCEPTION,
                                        "Unknown field '",
                                        "' in " + qname);
                                fromBody.endControlFlow();
                                fromBody.endControlFlow();
                                for (var c : info.components) {
                                    var key = CodecEmitter.toSnakeCase(c.name);
                                    fromBody.addStatement(
                                            "if (__$L.optIsNone()) throw new $T($S, path.field($S))",
                                            c.name,
                                            CodecEmitter.DESERIALIZATION_EXCEPTION,
                                            "Missing required field '" + c.name + "'",
                                            key);
                                }
                                var ctorArgs = CodeBlock.builder();
                                for (var i = 0; i < info.components.size(); i++) {
                                    if (i > 0) ctorArgs.add(", ");
                                    ctorArgs.add("__$L.optUnwrap()", info.components.get(i).name);
                                }
                                fromBody.addStatement("return new $L($L)", qname, ctorArgs.build());
                            } else if (info.kind == TypeKind.SEALED) {
                                toBody.addStatement(
                                        "$T<String, $T> __o = new $T<>()",
                                        CodecEmitter.TORDERED_MAP,
                                        CodecEmitter.JSON_VALUE,
                                        CodecEmitter.TORDERED_MAP);
                                for (var perm : info.permittedSubclasses) {
                                    var key = CodecEmitter.toSnakeCase(CodecEmitter.lastSegment(perm));
                                    toBody.beginControlFlow("if (value instanceof $L __v)", perm);
                                    toBody.addStatement(
                                            "__o.put($S, $L.toValue(__v))",
                                            key,
                                            CodecEmitter.codecTargetFor(perm).klass());
                                    toBody.nextControlFlow("else");
                                }
                                toBody.addStatement(
                                        "throw new IllegalStateException($S + value.getClass())",
                                        "Unhandled variant of " + qname + ": ");
                                for (var i = 0; i < info.permittedSubclasses.size(); i++) {
                                    toBody.endControlFlow();
                                }
                                toBody.addStatement("return new $T(__o)", CodecEmitter.JSON_VALUE_OBJECT);

                                fromBody.addStatement("var __o = $T.object(value, path)", CodecEmitter.JSON_VALUES);
                                fromBody.addStatement(
                                        "if (__o.size() != 1) throw new $T($S, path)",
                                        CodecEmitter.DESERIALIZATION_EXCEPTION,
                                        "Sealed " + qname + " must have exactly one variant");
                                fromBody.addStatement("var __en = __o.entrySet().iterator().next()");
                                fromBody.addStatement("String __tag = __en.getKey()");
                                fromBody.addStatement("$L __out", qname);
                                fromBody.beginControlFlow("switch (__tag)");
                                for (var perm : info.permittedSubclasses) {
                                    var key = CodecEmitter.toSnakeCase(CodecEmitter.lastSegment(perm));
                                    var ct = CodecEmitter.codecTargetFor(perm);
                                    if (ct.castTo().optIsSome()) {
                                        fromBody.add(
                                                "case $S -> __out = ($L) $L.fromValue(__en.getValue(), path.field($S));\n",
                                                key,
                                                ct.castTo().optUnwrap(),
                                                ct.klass(),
                                                key);
                                    } else {
                                        fromBody.add(
                                                "case $S -> __out = $L.fromValue(__en.getValue(), path.field($S));\n",
                                                key,
                                                ct.klass(),
                                                key);
                                    }
                                }
                                fromBody.add(
                                        "default -> throw new $T($S + __tag + $S, path);\n",
                                        CodecEmitter.DESERIALIZATION_EXCEPTION,
                                        "Unknown variant '",
                                        "' for " + qname);
                                fromBody.endControlFlow();
                                fromBody.addStatement("return __out");
                            } else {
                                toBody.addStatement(
                                        "return new $T(value.name().toLowerCase(java.util.Locale.ROOT))",
                                        CodecEmitter.JSON_VALUE_STRING);
                                fromBody.addStatement("String __s = $T.string(value, path)", CodecEmitter.JSON_VALUES);
                                fromBody.addStatement("String __u = __s.toUpperCase(java.util.Locale.ROOT)");
                                fromBody.beginControlFlow("for ($L __c : $L.values())", qname, qname);
                                fromBody.addStatement("if (__c.name().equals(__u)) return __c");
                                fromBody.endControlFlow();
                                fromBody.addStatement(
                                        "throw new $T($S + __s + $S, path)",
                                        CodecEmitter.DESERIALIZATION_EXCEPTION,
                                        "Unknown enum constant '",
                                        "' for " + qname);
                            }
                            cls.addMethod(MethodSpec.methodBuilder("toValue")
                                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                    .returns(CodecEmitter.JSON_VALUE)
                                    .addParameter(target, "value")
                                    .addCode(toBody.build())
                                    .build());
                            cls.addMethod(MethodSpec.methodBuilder("fromValue")
                                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                    .returns(target)
                                    .addParameter(CodecEmitter.JSON_VALUE, "value")
                                    .addParameter(CodecEmitter.FIELD_PATH, "path")
                                    .addCode(fromBody.build())
                                    .build());
                            cls.addMethod(MethodSpec.methodBuilder("fromValue")
                                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                    .returns(target)
                                    .addParameter(CodecEmitter.JSON_VALUE, "value")
                                    .addStatement("return fromValue(value, $T.root())", CodecEmitter.FIELD_PATH)
                                    .build());
                        }
                        if (info.implementsStreamMarker) {
                            var body = CodeBlock.builder();
                            body.addStatement(
                                    "$T __reader = new $T(new $T(json))",
                                    CodecEmitter.JSON_READER,
                                    CodecEmitter.JSON_READER,
                                    CodecEmitter.STRING_READER);
                            body.addStatement("return decode(__reader, $T.root())", CodecEmitter.FIELD_PATH);
                            cls.addMethod(MethodSpec.methodBuilder("deserialize")
                                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                    .returns(target)
                                    .addParameter(String.class, "json")
                                    .addException(CodecEmitter.IO_EXCEPTION)
                                    .addCode(body.build())
                                    .build());
                        }

                        var javaFile = JavaFile.builder(CodecsGen.GEN_PACKAGE, cls.build())
                                .skipJavaLangImports(true)
                                .build();
                        {
                            emit = javaFile.toString();
                            break _emit;
                        }
                    }
                    String src = emit;

                    String source = src;

                    var className = CodecEmitter.simpleNameOf(qname) + "Codec";
                    var fqcn = CodecsGen.GEN_PACKAGE + "." + className;

                    String formatted;
                    try {
                        formatted = formatter.formatSource(source);
                    } catch (FormatterException e) {
                        throw new IOException(
                                "palantir-java-format failed for " + fqcn + "\n--- source ---\n" + source, e);
                    }
                    var slash = fqcn.lastIndexOf('.');
                    var pkg = fqcn.substring(0, slash);
                    var simple = fqcn.substring(slash + 1);
                    var pkgDir = outRoot.resolve(pkg.replace('.', '/'));
                    Files.createDirectories(pkgDir);
                    var path = pkgDir.resolve(simple + ".java");
                    Files.writeString(path, formatted, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                }
            }
        }

        var codegenDescriptors = CodegenDescriptorScanner.scanRoot(runtimeSourceRoot);
        codegenDescriptors.addAll(CodegenDescriptorScanner.scanRoot(utilSourceRoot));
        TList<CodegenDescriptorScanner.Entry> entries = codegenDescriptors;
        {
            var cls = TypeSpec.classBuilder("GenCodegenDescriptors")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addMethod(MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PRIVATE)
                            .build());

            var sortedEntries = TList.from(entries);
            sortedEntries.sort((a, b) -> a.constantName().compareTo(b.constantName()));
            for (var e : sortedEntries) {
                var descArgs = CodeBlock.builder();
                descArgs.add("$T.fromClass($T.class)", DATA_DESCRIPTOR, e.returnType());
                for (var p : e.paramTypes()) {
                    descArgs.add(", $T.fromClass($T.class)", DATA_DESCRIPTOR, p);
                }
                var refClass = e.isStatic() ? STATIC_METHOD_REF : VIRTUAL_METHOD_REF;
                var init = CodeBlock.of(
                        "new $T($T.fromClass($T.class), $S, $T.fromParts($L), $L)",
                        refClass,
                        INTERNAL_NAME,
                        e.owner(),
                        e.methodName(),
                        METHOD_DESCRIPTOR,
                        descArgs.build(),
                        e.ownerIsInterface());
                cls.addField(
                        FieldSpec.builder(refClass, e.constantName(), Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                .initializer(init)
                                .build());
            }

            var file = JavaFile.builder("compilerxyz.main.gen", cls.build())
                    .skipJavaLangImports(true)
                    .build();
            GenMain.write(outRoot, file, formatter);
        }
        var sortedRoots = TList.<String>from(dispatchRoots);
        sortedRoots.sort(Comparator.naturalOrder());
        Files.writeString(
                outRoot.resolve(DISPATCH_ROOTS_FILE),
                String.join("\n", sortedRoots) + "\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static final String DISPATCH_ROOTS_FILE = "dispatch-roots.txt";

    static void write(Path outRoot, JavaFile file, Formatter formatter) throws IOException {
        var raw = file.toString();
        String formatted;
        try {
            formatted = formatter.formatSource(raw);
        } catch (FormatterException e) {
            throw new IOException("palantir-java-format failed for " + file.typeSpec.name, e);
        }
        var pkgDir = outRoot.resolve(file.packageName.replace('.', '/'));
        Files.createDirectories(pkgDir);
        var path = pkgDir.resolve(file.typeSpec.name + ".java");
        Files.writeString(path, formatted, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
