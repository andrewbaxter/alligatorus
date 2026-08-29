package compilerxyz.codegen;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
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
import compilerxyz.codegen.experimental.BuiltinPushStructScanner;
import compilerxyz.codegen.experimental.CodegenDescriptorScanner;
import compilerxyz.codegen.experimental.DynamicTypeScanner;
import compilerxyz.codegen.experimental.JavaAspectExtractor;
import compilerxyz.codegen.util.BuiltinMethodEmit;
import compilerxyz.codegen.util.BuiltinMethodEmit.Method;
import compilerxyz.codegen.util.BuiltinMethodEmit.Param;
import compilerxyz.codegen.util.DynamicTypes;
import compilerxyz.codegen.util.NameDerivation;
import compilerxyz.codegen.util.NamedTypeRef;
import compilerxyz.codegen.util.PushMethod;
import compilerxyz.codegen.util.TypeRef;
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
import java.util.Set;
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

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Expected three args: output source root, runtime source root, util source root");
        }
        var outRoot = Path.of(args[0]);
        var runtimeSourceRoot = Path.of(args[1]);
        var utilSourceRoot = Path.of(args[2]);
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

        TMap<ClassName, CodeBlock> _rScanRoot1;

        _scanRoot:
        {
            var out = new TMap<ClassName, CodeBlock>();
            try (Stream<Path> stream = Files.walk(runtimeSourceRoot)) {
                var files = stream.filter(p -> p.toString().endsWith(".java")).toList();
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
                        cn.simpleNames().subList(1, cn.simpleNames().size()).toArray(new String[0]));
                mirror.put(mirrored, e.getValue());
            }
            for (var e : mirror.entrySet()) out.put(e.getKey(), e.getValue());
            {
                _rScanRoot1 = out;
                break _scanRoot;
            }
        }

        TMap<ClassName, CodeBlock> scannedLeaves = _rScanRoot1;
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
                method.addStatement("case $S: return $T.some($L)", cn.reflectionName(), OPT, scannedLeaves.get(cn));
            }
            method.addStatement("default: return $T.none()", OPT);
            method.endControlFlow();
            cls.addMethod(method.build());

            var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
            GenMain.write(outRoot, file, formatter);
        }

        JavaAspectExtractor.ScanResult _rScanRoot2;

        _scanRoot:
        {
            var builtins = new TList<BuiltinInterfacesGen.BuiltinSpec>();
            try (Stream<Path> stream = Files.walk(runtimeSourceRoot)) {
                var files = stream.filter(p -> p.toString().endsWith(".java")).toList();
                for (var file : files) {
                    var src = Files.readString(file);
                    if (!src.contains("@BuiltinFull(")) continue;
                    var cu = StaticJavaParser.parse(file);
                    Opt<TypeDeclaration<?>> _rFindBuiltinType;
                    _findBuiltinType:
                    {
                        for (var t : cu.getTypes()) {
                            var hasBuiltin =
                                    t.getAnnotationByName("BuiltinFull").isPresent()
                                            || t.getAnnotationByName("compilerxyz.prelude.annotations.BuiltinFull")
                                                    .isPresent();
                            if (!hasBuiltin) continue;
                            if (t instanceof ClassOrInterfaceDeclaration c) {
                                _rFindBuiltinType = Opt.some(c);
                                break _findBuiltinType;
                            }
                            if (t instanceof RecordDeclaration r) {
                                _rFindBuiltinType = Opt.some(r);
                                break _findBuiltinType;
                            }
                        }
                        {
                            _rFindBuiltinType = Opt.none();
                            break _findBuiltinType;
                        }
                    }
                    Opt<TypeDeclaration<?>> declOpt = _rFindBuiltinType;

                    if (declOpt.optIsSome()) {
                        var decl = declOpt.optUnwrap();
                        Opt<BuiltinInterfacesGen.BuiltinSpec> builtin;
                        if (decl instanceof ClassOrInterfaceDeclaration coid) {
                            if (coid.isInterface()) {
                                ClassOrInterfaceDeclaration ifaceDecl = coid;
                                boolean _rInterfaceExtendsAspect;
                                _interfaceExtendsAspect:
                                {
                                    for (var ext : coid.getExtendedTypes()) {
                                        var n = ext.getNameAsString();
                                        if (n.equals("Aspect") || n.equals("compilerxyz.main.eval.aspect.Aspect")) {
                                            _rInterfaceExtendsAspect = true;
                                            break _interfaceExtendsAspect;
                                        }
                                    }
                                    {
                                        _rInterfaceExtendsAspect = false;
                                        break _interfaceExtendsAspect;
                                    }
                                }
                                boolean isAspect = _rInterfaceExtendsAspect;

                                Opt<BuiltinInterfacesGen.BuiltinSpec> _rExtractBuiltinSpecInterface;
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
                                        String _rDeriveSourceName;
                                        _deriveSourceName:
                                        {
                                            var prefix = Character.toLowerCase(interfaceSimpleName.charAt(0))
                                                    + interfaceSimpleName.substring(1);
                                            if (javaMethodName.startsWith(prefix)
                                                    && javaMethodName.length() > prefix.length()) {
                                                var rest = javaMethodName.substring(prefix.length());
                                                {
                                                    _rDeriveSourceName =
                                                            Character.toLowerCase(rest.charAt(0)) + rest.substring(1);
                                                    break _deriveSourceName;
                                                }
                                            }
                                            {
                                                _rDeriveSourceName = javaMethodName;
                                                break _deriveSourceName;
                                            }
                                        }
                                        var sourceName = NameDerivation.snakeCase(_rDeriveSourceName);
                                        var params = new TList<Param>();
                                        for (var p : m.getParameters()) {
                                            String name = p.getNameAsString();
                                            String _rStripReservedSuffix;
                                            _stripReservedSuffix:
                                            {
                                                if (name.endsWith("_") && name.length() > 1) {
                                                    _rStripReservedSuffix = name.substring(0, name.length() - 1);
                                                    break _stripReservedSuffix;
                                                }
                                                {
                                                    _rStripReservedSuffix = name;
                                                    break _stripReservedSuffix;
                                                }
                                            }
                                            params.add(new Param(
                                                    NameDerivation.snakeCase(_rStripReservedSuffix),
                                                    JavaAspectExtractor.typeNameOf(
                                                            p.getType(), effectiveImports, pkg)));
                                        }
                                        methods.add(new Method(
                                                sourceName,
                                                m.getNameAsString(),
                                                params,
                                                JavaAspectExtractor.typeNameOf(m.getType(), effectiveImports, pkg),
                                                true));
                                    }
                                    {
                                        _rExtractBuiltinSpecInterface = Opt.some(new BuiltinInterfacesGen.BuiltinSpec(
                                                simpleName,
                                                pkg,
                                                uniqueId,
                                                identityId,
                                                isAspect,
                                                true,
                                                methods,
                                                new TList<BuiltinInterfacesGen.DataField>()));
                                        break _extractBuiltinSpecInterface;
                                    }
                                }
                                builtin = _rExtractBuiltinSpecInterface;

                            } else {
                                ClassOrInterfaceDeclaration clsDecl = coid;
                                Opt<BuiltinInterfacesGen.BuiltinSpec> _rExtractBuiltinSpecClass;
                                _extractBuiltinSpecClass:
                                {
                                    var simpleName = clsDecl.getNameAsString();
                                    var pkg = cu.getPackageDeclaration()
                                            .orElseThrow()
                                            .getNameAsString();
                                    var builtinAnn = JavaAspectExtractor.getBuiltinAnnotation(clsDecl);
                                    var attrs = JavaAspectExtractor.readAttrs(builtinAnn);
                                    var uniqueIdStr = attrs.get("uniqueId");
                                    if (uniqueIdStr == null) {
                                        throw new IllegalStateException(
                                                "@Builtin on " + simpleName + " missing required uniqueId");
                                    }
                                    var uniqueId = Long.parseLong(uniqueIdStr);
                                    {
                                        _rExtractBuiltinSpecClass = Opt.some(new BuiltinInterfacesGen.BuiltinSpec(
                                                simpleName,
                                                pkg,
                                                uniqueId,
                                                0L,
                                                false,
                                                false,
                                                new TList<Method>(),
                                                new TList<BuiltinInterfacesGen.DataField>()));
                                        break _extractBuiltinSpecClass;
                                    }
                                }
                                builtin = _rExtractBuiltinSpecClass;
                            }
                        } else if (decl instanceof RecordDeclaration recDecl) {
                            builtin = JavaAspectExtractor.extractBuiltinSpecRecord(cu, recDecl, Opt.none());
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
                _rScanRoot2 = new JavaAspectExtractor.ScanResult(builtins);
                break _scanRoot;
            }
        }

        JavaAspectExtractor.ScanResult scan = _rScanRoot2;

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
                MethodSpec _rBuildBuild;
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
                                        + "$T.<$T>some(new $T($T.AspectId, $T.IdentityId)))",
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
                                        + "$T.<$T>none())",
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
                                        + "$T.DYNAMIC_LIGHT, $T.<$T>none())",
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
                                        + "$T.<$T>some(new $T(new $T($LL), new $T($LL))))",
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
                                spec.uniqueId());
                        if (spec.isInterface()) {
                            b.addStatement(
                                    "var instance = new $T(meta, jvmClass, jvmClassDesc, $T.class, "
                                            + "$T.DYNAMIC_LIGHT, $T.some(jvmClass))",
                                    BuiltinInterfacesGen.TYPE_OBJECT_DYNAMIC,
                                    interfaceRef,
                                    BuiltinInterfacesGen.OBJECT_DROP_KIND,
                                    BuiltinInterfacesGen.OPT);
                        } else {
                            b.addStatement(
                                    "var instance = new $T(meta, jvmClass, jvmClassDesc, $T.class, "
                                            + "$T.DYNAMIC_LIGHT, $T.<$T>none())",
                                    BuiltinInterfacesGen.TYPE_OBJECT_DYNAMIC,
                                    interfaceRef,
                                    BuiltinInterfacesGen.OBJECT_DROP_KIND,
                                    BuiltinInterfacesGen.OPT,
                                    BuiltinInterfacesGen.JAVA_INTERNAL_NAME);
                        }
                    }

                    b.addStatement("return instance");
                    {
                        _rBuildBuild = b.build();
                        break _buildBuild;
                    }
                }
                holder.addMethod(_rBuildBuild);
                cls.addType(holder.build());

                MethodSpec _rBuildLink;

                _buildLink:
                {
                    var b = MethodSpec.methodBuilder("link").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
                    b.addStatement("var instance = Holder.INSTANCE");
                    b.beginControlFlow("if (instance.methods() != null)");
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
                            var retDescOpt = BuiltinMethodEmit.resolveDescriptor(method.ret(), leafDescriptors);
                            if (retDescOpt.optIsNone()) break _emitObjectMethod;
                            var retDesc = retDescOpt.optUnwrap();
                            var paramDescs = new TList<CodeBlock>();
                            var paramRawClasses = new TList<TypeName>();
                            for (var p : method.params()) {
                                var pdOpt = BuiltinMethodEmit.resolveDescriptor(p.type(), leafDescriptors);
                                if (pdOpt.optIsNone()) break _emitObjectMethod;
                                paramDescs.add(pdOpt.optUnwrap());
                                var rawOpt = BuiltinMethodEmit.rawClass(p.type());
                                if (rawOpt.optIsNone()) break _emitObjectMethod;
                                paramRawClasses.add(rawOpt.optUnwrap());
                            }
                            var retRawOpt = BuiltinMethodEmit.rawClass(method.ret());
                            if (retRawOpt.optIsNone()) break _emitObjectMethod;
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
                                b.addStatement(
                                        "var $L = $T.<$T>some($L)",
                                        retTypeName,
                                        BuiltinMethodEmit.OPT,
                                        BuiltinMethodEmit.TYPE,
                                        retDesc);
                            }

                            var descName = "desc_" + suffix;
                            var argClassBuilder = CodeBlock.builder();
                            for (int i = 0; i < paramRawClasses.size(); i++) {
                                if (i > 0) argClassBuilder.add(", ");
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
                            b.addStatement(
                                    "var $L = new $T($N, $L, $L, $L, $S, $L, $T.class.isInterface(), false)",
                                    methodVar,
                                    BuiltinMethodEmit.TYPE_METHOD_DYNAMIC,
                                    receiverVar,
                                    argRecordName,
                                    retTypeName,
                                    jvmClassExpr,
                                    method.hostMethodName(),
                                    descName,
                                    hostClass);
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
                                            "compilerxyz.main.eval.value.object_",
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
                        _rBuildLink = b.build();
                        break _buildLink;
                    }
                }

                cls.addMethod(_rBuildLink);

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
        TList<BuiltinPathScanner.Entry> _rScanRoot3;
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
                    TMap<String, String> _rBuildImportMap;
                    _buildImportMap:
                    {
                        var m = new TMap<String, String>();
                        for (var imp : cu.getImports()) {
                            var full = imp.getNameAsString();
                            var simple = full.substring(full.lastIndexOf('.') + 1);
                            m.put(simple, full);
                        }
                        {
                            _rBuildImportMap = m;
                            break _buildImportMap;
                        }
                    }
                    TMap<String, String> imports = _rBuildImportMap;

                    for (var t : cu.getTypes()) {
                        BuiltinPathScanner.walkType(pkg, pkg, "", t, imports, out, scannedLeaves);
                    }
                }
            }
            {
                _rScanRoot3 = out;
                break _scanRoot;
            }
        }
        TList<BuiltinPathScanner.Entry> pathEntries = _rScanRoot3;

        TList<BuiltinIdentityScanner.IdentitySpec> _rScanRoot4;

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
                _rScanRoot4 = out;
                break _scanRoot;
            }
        }

        TList<BuiltinIdentityScanner.IdentitySpec> scannedIdentities = _rScanRoot4;

        var identityExtras = new TList<IdentitySpec>();
        for (var s : scannedIdentities) {
            identityExtras.add(new IdentitySpec(s.name(), s.value(), s.standalone()));
        }
        TList<IdentitySpec> _rIdentitySpecs;
        _identitySpecs:
        {
            var out = new TList<IdentitySpec>();
            for (var prim : ListsGen.PRIMS) {
                out.add(new IdentitySpec(ListsGen.snake(ListsGen.typeSimple(prim)), prim.uniqueId(), true));
            }
            {
                _rIdentitySpecs = out;
                break _identitySpecs;
            }
        }
        identityExtras.addAll(_rIdentitySpecs);
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
                String _rConstantName;
                _constantName:
                {
                    var s = snake;
                    if (s.endsWith("_identity")) s = s.substring(0, s.length() - "_identity".length());
                    {
                        _rConstantName = s.toUpperCase(Locale.ROOT) + "_IDENTITY";
                        break _constantName;
                    }
                }
                cls.addField(FieldSpec.builder(UNIQUE, _rConstantName, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T($LL)", UNIQUE_BUILTIN, spec.builtinUniqueId())
                        .build());
            }

            var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
            GenMain.write(outRoot, file, formatter);
        }
        {
            for (var store : ListsGen.STORES) {
                TypeSpec _rStoreClass;
                _storeClass:
                {
                    var arrayType = ArrayTypeName.of(store.javaPrim());
                    {
                        _rStoreClass = TypeSpec.classBuilder(store.simple())
                                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
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
                                .addMethod(MethodSpec.methodBuilder("pop")
                                        .addModifiers(Modifier.PUBLIC)
                                        .returns(store.javaPrim())
                                        .addStatement("size -= 1")
                                        .addStatement("return data[size]")
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
                                .build();
                        break _storeClass;
                    }
                }
                GenMain.write(
                        outRoot,
                        JavaFile.builder(ListsGen.GEN_PACKAGE, _rStoreClass).build(),
                        formatter);
            }
            TypeSpec _rFactoryClass;
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
                    _rFactoryClass = TypeSpec.classBuilder("GenLists")
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
                    JavaFile.builder(ListsGen.GEN_PACKAGE, _rFactoryClass).build(),
                    formatter);
        }

        TList<PushStructsGen.PushStructSpec> _rScanRoot;

        _scanRoot:
        {
            var out = new TList<PushStructsGen.PushStructSpec>();
            try (Stream<Path> stream = Files.walk(runtimeSourceRoot)) {
                var files = stream.filter(p -> p.toString().endsWith(".java")).toList();
                for (var file : files) {
                    var src = Files.readString(file);
                    if (!src.contains("BuiltinPushStruct")) continue;
                    var cu = StaticJavaParser.parse(file);
                    TMap<String, String> _rBuildImportMap;
                    _buildImportMap:
                    {
                        var m = new TMap<String, String>();
                        for (var imp : cu.getImports()) {
                            var full = imp.getNameAsString();
                            var simple = full.substring(full.lastIndexOf('.') + 1);
                            m.put(simple, full);
                        }
                        {
                            _rBuildImportMap = m;
                            break _buildImportMap;
                        }
                    }
                    TMap<String, String> imports = _rBuildImportMap;

                    var pkg = cu.getPackageDeclaration().orElseThrow().getNameAsString();
                    for (var t : cu.getTypes()) {
                        if (!(t instanceof ClassOrInterfaceDeclaration c) || c.isInterface()) continue;
                        AnnotationExpr ann = null;
                        for (var a : c.getAnnotations()) {
                            var n = a.getNameAsString();
                            if (n.equals("BuiltinPushStruct")
                                    || n.equals("compilerxyz.prelude.annotations.BuiltinPushStruct")) {
                                ann = a;
                                break;
                            }
                        }
                        if (ann == null) continue;
                        ClassOrInterfaceDeclaration cls = c;
                        PushStructsGen.PushStructSpec _rExtractStruct;
                        _extractStruct:
                        {
                            var name = cls.getNameAsString();

                            var dataFields = new TList<NamedTypeRef>();
                            var ctors = cls.getConstructors();
                            if (!ctors.isEmpty()) {
                                var ctor = ctors.get(0);
                                for (var p : ctor.getParameters()) {
                                    var srcName = BuiltinPushStructScanner.snakeStripTrailing(p.getNameAsString());
                                    dataFields.add(new NamedTypeRef(
                                            srcName,
                                            BuiltinPushStructScanner.typeRefOf(
                                                    p.getType(), imports, pkg, scannedLeaves)));
                                }
                            }

                            var fields = new TList<PushStructsGen.PushField>();
                            for (var f : cls.getFields()) {
                                if (f.isStatic()
                                        || !f.hasModifier(com.github.javaparser.ast.Modifier.Keyword.PUBLIC)
                                        || !f.isFinal()) continue;
                                for (var v : f.getVariables()) {
                                    var javaName = v.getNameAsString();
                                    var srcName = BuiltinPushStructScanner.snakeStripTrailing(javaName);
                                    var tr = BuiltinPushStructScanner.typeRefOf(
                                            f.getElementType(), imports, pkg, scannedLeaves);
                                    fields.add(new PushStructsGen.PushField(srcName, javaName, tr, CodeBlock.of("")));
                                }
                            }

                            var methods = new TList<PushMethod>();
                            for (var m : cls.getMethods()) {
                                if (m.isStatic() || !m.hasModifier(com.github.javaparser.ast.Modifier.Keyword.PUBLIC))
                                    continue;
                                var params = m.getParameters();
                                var srcMethodName = BuiltinPushStructScanner.snakeStripTrailing(m.getNameAsString());
                                var needsEvalContext = false;
                                if (!params.isEmpty()
                                        && params.get(0).getNameAsString().equals("vx")) {
                                    Type pt = params.get(0).getType();
                                    boolean _rIsEvalContext;
                                    _isEvalContext:
                                    {
                                        if (!(pt instanceof ClassOrInterfaceType cit)) {
                                            _rIsEvalContext = false;
                                            break _isEvalContext;
                                        }
                                        var fqn = BuiltinPushStructScanner.resolve(cit.getNameAsString(), imports, pkg);
                                        _rIsEvalContext = fqn.equals("compilerxyz.main.eval.EvalContext");
                                    }
                                    needsEvalContext = _rIsEvalContext;
                                }
                                var methodArgs = new TList<NamedTypeRef>();
                                for (int i = needsEvalContext ? 1 : 0; i < params.size(); i++) {
                                    var p = params.get(i);
                                    var argSrcName = BuiltinPushStructScanner.snakeStripTrailing(p.getNameAsString());
                                    methodArgs.add(new NamedTypeRef(
                                            argSrcName,
                                            BuiltinPushStructScanner.typeRefOf(
                                                    p.getType(), imports, pkg, scannedLeaves)));
                                }
                                Type rt = m.getType();
                                String _rReturnExpr;
                                _returnExpr:
                                {
                                    if (rt instanceof VoidType) {
                                        _rReturnExpr = "()";
                                        break _returnExpr;
                                    }
                                    var tr = BuiltinPushStructScanner.typeRefOf(rt, imports, pkg, scannedLeaves);
                                    {
                                        _rReturnExpr = tr.dynamicType().toString();
                                        break _returnExpr;
                                    }
                                }
                                String retExpr = _rReturnExpr;

                                TypeRef _rReturnTypeRef;

                                _returnTypeRef:
                                {
                                    if (rt instanceof VoidType) {
                                        _rReturnTypeRef = TypeRef.opaque(TypeName.VOID);
                                        break _returnTypeRef;
                                    }
                                    {
                                        _rReturnTypeRef =
                                                BuiltinPushStructScanner.typeRefOf(rt, imports, pkg, scannedLeaves);
                                        break _returnTypeRef;
                                    }
                                }

                                TypeRef retClass = _rReturnTypeRef;

                                methods.add(new PushMethod(
                                        srcMethodName,
                                        methodArgs,
                                        retExpr,
                                        CodeBlock.of(""),
                                        "",
                                        retClass,
                                        needsEvalContext));
                            }

                            {
                                _rExtractStruct = new PushStructsGen.PushStructSpec(name, dataFields, fields, methods);
                                break _extractStruct;
                            }
                        }
                        out.add(_rExtractStruct);
                    }
                }
            }
            {
                _rScanRoot = out;
                break _scanRoot;
            }
        }

        TList<PushStructsGen.PushStructSpec> scannedPushStructs = _rScanRoot;

        TList<PushStructsGen.PushStructSpec> _rSpecs;
        _specs:
        {
            {
                _rSpecs = TList.of();
                break _specs;
            }
        }
        {
            TList<PushStructsGen.PushStructSpec> specs = _rSpecs;
            TList<PushStructsGen.PushStructSpec> extraSpecs = scannedPushStructs;

            var all = new TList<PushStructsGen.PushStructSpec>();
            all.addAll(specs);
            all.addAll(extraSpecs);
            var names = new TSet<String>();
            for (var spec : all) {
                if (!names.add(spec.name())) {
                    throw new IllegalStateException("duplicate push struct name: " + spec.name());
                }
                var memberNames = new TSet<String>();
                for (var f : spec.fields()) {
                    if (!memberNames.add(PushStructsGen.pushMethodName(f.name()))) {
                        throw new IllegalStateException(
                                "duplicate field/method name on " + spec.name() + ": " + f.name());
                    }
                }
                for (var m : spec.methods()) {
                    if (!memberNames.add(PushStructsGen.pushMethodName(m.name()))) {
                        throw new IllegalStateException(
                                "duplicate field/method name on " + spec.name() + ": " + m.name());
                    }
                }
            }

            for (var spec : all) {
                if (PushStructsGen.HANDWRITTEN_STRUCTS.contains(spec.name())) continue;

                var chars = spec.name().toCharArray();
                chars[0] = Character.toLowerCase(chars[0]);
                var factoryName = new String(chars);
                var genObjectTypes = ClassName.get(PushStructsGen.GEN_PACKAGE, "GenObjectTypes");
                var cls = TypeSpec.classBuilder(spec.name()).addModifiers(Modifier.PUBLIC, Modifier.FINAL);

                var ctor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
                for (var df : spec.dataFields()) {
                    cls.addField(FieldSpec.builder(df.type().path(), df.name(), Modifier.PUBLIC, Modifier.FINAL)
                            .build());
                    if (df.derivedFrom().optIsSome()) {
                        ctor.addStatement(
                                "this.$N = $L", df.name(), df.derivedFrom().optUnwrap());
                    } else {
                        ctor.addParameter(ParameterSpec.builder(df.type().path(), df.name())
                                .build());
                        ctor.addStatement("this.$N = $N", df.name(), df.name());
                    }
                }
                cls.addMethod(ctor.build());

                cls.addMethod(MethodSpec.methodBuilder("dynamicType")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(PushStructsGen.TYPE_OBJECT_DYNAMIC)
                        .addStatement("return $T.$L()", genObjectTypes, factoryName)
                        .build());

                var file = JavaFile.builder(PushStructsGen.GEN_PACKAGE, cls.build())
                        .build();
                GenMain.write(outRoot, file, formatter);
            }
            var cls = TypeSpec.classBuilder("GenObjectTypes").addModifiers(Modifier.PUBLIC, Modifier.FINAL);
            cls.addMethod(MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .build());
            for (var spec : all) {
                var chars = spec.name().toCharArray();
                chars[0] = Character.toLowerCase(chars[0]);
                var fieldName = new String(chars) + "_instance";
                cls.addField(FieldSpec.builder(
                                PushStructsGen.TYPE_OBJECT_DYNAMIC, fieldName, Modifier.PRIVATE, Modifier.STATIC)
                        .build());
                MethodSpec _rBuildObjectTypeFactory;
                _buildObjectTypeFactory:
                {
                    var nameChars = spec.name().toCharArray();
                    nameChars[0] = Character.toLowerCase(nameChars[0]);
                    var factoryName = new String(nameChars);
                    var specClass = ClassName.get(PushStructsGen.GEN_PACKAGE, spec.name());
                    var jvmInternal = specClass.canonicalName().replace('.', '/');

                    var dataFieldsList = CodeBlock.builder();
                    dataFieldsList.add("$T.<$T>of(", PushStructsGen.TLIST, PushStructsGen.OBJECT_DATA_FIELD);
                    for (int i = 0; i < spec.fields().size(); i++) {
                        var f = spec.fields().get(i);
                        if (i > 0) dataFieldsList.add(", ");
                        dataFieldsList.add(
                                "$T.ofField($S, $S, $T.fromClass($T.class), $L)",
                                PushStructsGen.OBJECT_DATA_FIELD,
                                PushStructsGen.pushMethodName(f.name()),
                                f.javaFieldName(),
                                PushStructsGen.JAVA_DATA_DESCRIPTOR,
                                PushStructsGen.rawTypeName(f.ret().path()),
                                f.ret().dynamicType());
                    }
                    dataFieldsList.add(")");

                    var isSelfReferential = PushStructsGen.INSTANCE_METHOD_STRUCTS.contains(spec.name());
                    var methodsList = CodeBlock.builder();
                    if (isSelfReferential) {
                        methodsList.add("$N", "__methods");
                    } else {
                        methodsList.add("$T.<$T>of(", PushStructsGen.TLIST, PushStructsGen.OBJECT_METHOD);
                        for (int i = 0; i < spec.methods().size(); i++) {
                            var meth = spec.methods().get(i);
                            var dynRuntimeMethod = meth.dynRuntimeMethod();
                            if (dynRuntimeMethod == null
                                    && !PushStructsGen.INSTANCE_METHOD_STRUCTS.contains(spec.name())) {
                                throw new IllegalStateException(
                                        "PushMethod '" + meth.name() + "' has no dynRuntimeMethod set");
                            }
                            if (i > 0) methodsList.add(", ");
                            methodsList.add(
                                    "new $T($S, $L)",
                                    PushStructsGen.OBJECT_METHOD,
                                    PushStructsGen.pushMethodName(meth.name()),
                                    PushStructsGen.methodFunctionType(spec, specClass, meth, dynRuntimeMethod));
                        }
                        methodsList.add(")");
                    }

                    var builder = MethodSpec.methodBuilder(factoryName)
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .returns(PushStructsGen.TYPE_OBJECT_DYNAMIC)
                            .beginControlFlow("if ($N == null)", fieldName);
                    if (isSelfReferential) {
                        builder.addStatement(
                                "var __methods = new $T<$T>()", PushStructsGen.TLIST, PushStructsGen.OBJECT_METHOD);
                    }
                    builder.addStatement(
                            "var __meta = new $T($S, $T.<$T>of(), $T.<$T>of(), $T.<$T>of(), $T.none())",
                            PushStructsGen.TYPE_OBJECT_META,
                            spec.name(),
                            PushStructsGen.ILIST,
                            PushStructsGen.UNIQUE,
                            PushStructsGen.ILIST,
                            PushStructsGen.TYPE_OBJECT_CAST_RULE,
                            PushStructsGen.ILIST,
                            PushStructsGen.TYPE_OBJECT_CAST_FROM_RULE,
                            PushStructsGen.OPT);
                    builder.addStatement(
                            "$N = new $T(__meta, $T.of($S), $T.fromClass($T.class), $T.class, "
                                    + "$T.DYNAMIC, $T.none())",
                            fieldName,
                            PushStructsGen.TYPE_OBJECT_DYNAMIC,
                            PushStructsGen.JAVA_INTERNAL_NAME,
                            jvmInternal,
                            PushStructsGen.JAVA_DATA_DESCRIPTOR,
                            specClass,
                            specClass,
                            PushStructsGen.OBJECT_DROP_KIND,
                            PushStructsGen.OPT);
                    if (!isSelfReferential) {
                        builder.addStatement(
                                "$N.linkInit($T.from($L), $T.from($L))",
                                fieldName,
                                PushStructsGen.ILIST,
                                dataFieldsList.build(),
                                PushStructsGen.ILIST,
                                methodsList.build());
                    }
                    if (isSelfReferential) {
                        for (var meth : spec.methods()) {
                            var dynRuntimeMethod = meth.dynRuntimeMethod();
                            if (dynRuntimeMethod == null
                                    && !PushStructsGen.INSTANCE_METHOD_STRUCTS.contains(spec.name())) {
                                throw new IllegalStateException(
                                        "PushMethod '" + meth.name() + "' has no dynRuntimeMethod set");
                            }
                            builder.addStatement(
                                    "__methods.add(new $T($S, $L))",
                                    PushStructsGen.OBJECT_METHOD,
                                    PushStructsGen.pushMethodName(meth.name()),
                                    PushStructsGen.methodFunctionType(spec, specClass, meth, dynRuntimeMethod));
                        }
                        builder.addStatement(
                                "$N.linkInit($T.from($L), $T.from(__methods))",
                                fieldName,
                                PushStructsGen.ILIST,
                                dataFieldsList.build(),
                                PushStructsGen.ILIST);
                    }
                    {
                        _rBuildObjectTypeFactory = builder.endControlFlow()
                                .addStatement("return $N", fieldName)
                                .build();
                        break _buildObjectTypeFactory;
                    }
                }
                cls.addMethod(_rBuildObjectTypeFactory);
            }
            var file = JavaFile.builder(PushStructsGen.GEN_PACKAGE, cls.build()).build();
            GenMain.write(outRoot, file, formatter);
        }
        {
            TList<String> linkClassNames = links;

            TList<BuiltinPathScanner.Entry> scannedPathEntries = pathEntries;

            Set<String> builtinInterfaceHostNames = hostNames;

            BuiltinTreeGen.NodeSpec _rCanonicalSpec;
            _canonicalSpec:
            {
                var pathToLeaf = new LinkedHashMap<String, BuiltinTreeGen.LeafSpec>();
                for (var entry : scannedPathEntries) {
                    BuiltinTreeGen.LeafSpec spec =
                            switch (entry) {
                                case BuiltinPathScanner.TypeLeaf tl -> {
                                    var host = tl.typeClass().canonicalName();
                                    var wrapper = builtinInterfaceHostNames.contains(host)
                                            ? BuiltinTreeGen.TOD_SELF_TYPE_EXPR
                                            : BuiltinTreeGen.TYPE_WRAPPER_EXPR;
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
                                    CodeBlock _rParamsExpr;
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
                                            _rParamsExpr = b.build();
                                            break _paramsExpr;
                                        }
                                    }
                                    var innerExpr = CodeBlock.of(
                                            "new $T($L, $L, $T.of($S), $S, $L)",
                                            BuiltinTreeGen.TYPE_FUNCTION_DYNAMIC,
                                            _rParamsExpr,
                                            retExpr,
                                            BuiltinTreeGen.JAVA_INTERNAL_NAME,
                                            fl.ownerInternalName(),
                                            fl.methodName(),
                                            fl.needsEvalContext());
                                    yield new BuiltinTreeGen.LeafSpec(innerExpr, BuiltinTreeGen.TYPE_WRAPPER_EXPR);
                                }
                            };
                    if (pathToLeaf.put(entry.entryPath(), spec) != null) {
                        throw new IllegalStateException("duplicate @BuiltinPath entry: " + entry.entryPath());
                    }
                }
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_drop_dynamic_light",
                        CodeBlock.of(
                                "$T.dynamicType()", ClassName.get("compilerxyz.main.gen", "GenAspectDropDynamicLight")),
                        BuiltinTreeGen.TOD_SELF_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_initial_bind",
                        CodeBlock.of("$T.dynamicType()", ClassName.get("compilerxyz.main.gen", "GenAspectInitialBind")),
                        BuiltinTreeGen.TOD_SELF_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_lift",
                        CodeBlock.of("$T.dynamicType()", ClassName.get("compilerxyz.main.gen", "GenAspectLift")),
                        BuiltinTreeGen.TOD_SELF_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_bind_dynamic",
                        CodeBlock.of("$T.dynamicType()", ClassName.get("compilerxyz.main.gen", "GenAspectBindDynamic")),
                        BuiltinTreeGen.TOD_SELF_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_consume_dynamic",
                        CodeBlock.of(
                                "$T.dynamicType()", ClassName.get("compilerxyz.main.gen", "GenAspectConsumeDynamic")),
                        BuiltinTreeGen.TOD_SELF_TYPE_EXPR);
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
                        "eval.aspect_drop_dynamic_light_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectDropDynamicLight")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_stack_value_dynamic_id",
                        CodeBlock.of(
                                "$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectStackValueDynamic")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_field_use_dynamic_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectFieldUseDynamic")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_element_use_dynamic_id",
                        CodeBlock.of(
                                "$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectElementUseDynamic")),
                        BuiltinTreeGen.TUC_TYPE_EXPR);
                BuiltinTreeGen.addSpecial(
                        pathToLeaf,
                        "eval.aspect_initial_bind_id",
                        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.main.gen", "GenAspectInitialBind")),
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
                    _rCanonicalSpec = BuiltinTreeGen.toNodeSpec(root, "");
                    break _canonicalSpec;
                }
            }
            BuiltinTreeGen.NodeSpec spec = _rCanonicalSpec;

            BuiltinTreeGen.NodeSpec root = spec;

            for (var id : new long[] {
                BuiltinTreeGen.ASPECT_COMPILE_SCOPE_ID,
                BuiltinTreeGen.CAST_VALUE_COMPILE_SCOPE_ID,
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

            MethodSpec _rInstallRootFactory;

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
                    _rInstallRootFactory = m.build();
                    break _installRootFactory;
                }
            }

            cls.addMethod(_rInstallRootFactory);

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

            for (var qname : closure) {
                if (CodecsGen.HAND_CODED.contains(qname)) continue;
                var t = index.byQname.get(qname);
                if (t == null) continue;
                if (t.kind == TypeKind.RECORD || t.kind == TypeKind.SEALED || t.kind == TypeKind.ENUM) {
                    CodecsGen.TypeInfo info = t;
                    String _rEmit;
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
                        TypeName _rClassNameOf;
                        _classNameOf:
                        {
                            var lastDot = qname.lastIndexOf('.');
                            if (lastDot < 0) {
                                {
                                    _rClassNameOf = ClassName.bestGuess(qname);
                                    break _classNameOf;
                                }
                            }
                            var pkg = qname.substring(0, lastDot);
                            var rest = qname.substring(lastDot + 1);
                            var parts = rest.split("\\.");
                            {
                                _rClassNameOf =
                                        ClassName.get(pkg, parts[0], Arrays.copyOfRange(parts, 1, parts.length));
                                break _classNameOf;
                            }
                        }
                        TypeName target = _rClassNameOf;

                        var cls = TypeSpec.classBuilder(codecClassName).addModifiers(Modifier.PUBLIC, Modifier.FINAL);

                        cls.addMethod(MethodSpec.constructorBuilder()
                                .addModifiers(Modifier.PRIVATE)
                                .build());

                        if (info.kind == TypeKind.RECORD) {
                            MethodSpec _rBuildRecordEncode;
                            _buildRecordEncode:
                            {
                                var body = CodeBlock.builder();
                                body.addStatement("writer.beginObject()");
                                body.beginControlFlow("if (style == $T.CANONICAL)", CodecEmitter.JSON_STYLE);
                                TList<Component> cs = info.components;
                                TList<Component> _rSortedBySnake;
                                _sortedBySnake:
                                {
                                    var out = TList.from(cs);
                                    out.sort((a, b) -> CodecEmitter.toSnakeCase(a.name)
                                            .compareTo(CodecEmitter.toSnakeCase(b.name)));
                                    {
                                        _rSortedBySnake = out;
                                        break _sortedBySnake;
                                    }
                                }
                                for (var c : _rSortedBySnake) {
                                    body.add(CodecEmitter.encodeFieldBlock(c));
                                }
                                body.nextControlFlow("else");
                                for (var c : info.components) {
                                    body.add(CodecEmitter.encodeFieldBlock(c));
                                }
                                body.endControlFlow();
                                body.addStatement("writer.endObject()");
                                {
                                    _rBuildRecordEncode = MethodSpec.methodBuilder("encode")
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
                            cls.addMethod(_rBuildRecordEncode);
                            MethodSpec _rBuildRecordDecode;
                            _buildRecordDecode:
                            {
                                var body = CodeBlock.builder();
                                for (var c : info.components) {
                                    body.addStatement(
                                            "$L __$L = $L",
                                            CodecEmitter.javaTypeOf(c.type),
                                            c.name,
                                            CodecEmitter.defaultValueExpr(c.type));
                                }
                                body.addStatement(
                                        "$T<String> __seen = new $T<>()", CodecEmitter.TSET, CodecEmitter.TSET);
                                body.addStatement("reader.beginObject()");
                                body.beginControlFlow("while (reader.hasNext())");
                                body.addStatement("String __fname = reader.nextName()");
                                body.beginControlFlow("switch (__fname)");
                                for (var c : info.components) {
                                    var key = CodecEmitter.toSnakeCase(c.name);
                                    body.beginControlFlow("case $S ->", key);
                                    body.addStatement("__seen.add($S)", key);
                                    body.addStatement("$T __p = path.field($S)", CodecEmitter.FIELD_PATH, key);
                                    body.add(CodecEmitter.decodeStmts(
                                            c.type, "reader", CodeBlock.of("__p"), "__" + c.name));
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
                                    if (!c.type.nullable) {
                                        var key = CodecEmitter.toSnakeCase(c.name);
                                        body.addStatement(
                                                "if (!__seen.contains($S)) throw new $T($S, path.field($S))",
                                                key,
                                                CodecEmitter.DESERIALIZATION_EXCEPTION,
                                                "Missing required field '" + c.name + "'",
                                                key);
                                    }
                                }
                                var ctorArgs = CodeBlock.builder();
                                for (var i = 0; i < info.components.size(); i++) {
                                    if (i > 0) ctorArgs.add(", ");
                                    ctorArgs.add("__$L", info.components.get(i).name);
                                }
                                body.addStatement("return new $L($L)", qname, ctorArgs.build());
                                {
                                    _rBuildRecordDecode = MethodSpec.methodBuilder("decode")
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
                            cls.addMethod(_rBuildRecordDecode);
                        } else if (info.kind == TypeKind.SEALED) {
                            MethodSpec _rBuildSealedEncode;
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
                                            CodecEmitter.codecTargetFor(perm).klass());
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
                                    _rBuildSealedEncode = MethodSpec.methodBuilder("encode")
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
                            cls.addMethod(_rBuildSealedEncode);
                            MethodSpec _rBuildSealedDecode;
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
                                            CodecEmitter.codecTargetFor(perm).klass(),
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
                                    _rBuildSealedDecode = MethodSpec.methodBuilder("decode")
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
                            cls.addMethod(_rBuildSealedDecode);
                        } else if (info.kind == TypeKind.ENUM) {
                            MethodSpec _rBuildEnumEncode;
                            _buildEnumEncode:
                            {
                                {
                                    _rBuildEnumEncode = MethodSpec.methodBuilder("encode")
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
                            cls.addMethod(_rBuildEnumEncode);
                            MethodSpec _rBuildEnumDecode;
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
                                    _rBuildEnumDecode = MethodSpec.methodBuilder("decode")
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
                            cls.addMethod(_rBuildEnumDecode);
                        } else {
                            throw new IllegalStateException("Unsupported kind: " + info.kind + " for " + qname);
                        }

                        if (info.implementsMarker) {
                            MethodSpec _rBuildToValue;
                            _buildToValue:
                            {
                                var body = CodeBlock.builder();
                                body.beginControlFlow("try");
                                body.addStatement(
                                        "$T __sw = new $T()", CodecEmitter.STRING_WRITER, CodecEmitter.STRING_WRITER);
                                body.addStatement(
                                        "$T __jw = new $T(__sw)", CodecEmitter.JSON_WRITER, CodecEmitter.JSON_WRITER);
                                body.addStatement("__jw.setSerializeNulls(true)");
                                body.addStatement("__jw.setIndent($S)", "");
                                body.addStatement("encode(__jw, value, $T.COMPACT)", CodecEmitter.JSON_STYLE);
                                body.addStatement("__jw.close()");
                                body.addStatement("return $T.parse(__sw.toString())", CodecEmitter.JSON_VALUES);
                                body.nextControlFlow("catch ($T __e)", CodecEmitter.IO_EXCEPTION);
                                body.addStatement("throw new AssertionError($S, __e)", "In-memory IO failed");
                                body.endControlFlow();
                                {
                                    _rBuildToValue = MethodSpec.methodBuilder("toValue")
                                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                            .returns(CodecEmitter.JSON_VALUE)
                                            .addParameter(target, "value")
                                            .addCode(body.build())
                                            .build();
                                    break _buildToValue;
                                }
                            }
                            cls.addMethod(_rBuildToValue);
                            MethodSpec _rBuildFromValue;
                            _buildFromValue:
                            {
                                var body = CodeBlock.builder();
                                body.beginControlFlow("try");
                                body.addStatement("return deserialize($T.serialize(value))", CodecEmitter.JSON_VALUES);
                                body.nextControlFlow("catch ($T __e)", CodecEmitter.IO_EXCEPTION);
                                body.addStatement("throw new AssertionError($S, __e)", "In-memory IO failed");
                                body.endControlFlow();
                                {
                                    _rBuildFromValue = MethodSpec.methodBuilder("fromValue")
                                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                            .returns(target)
                                            .addParameter(CodecEmitter.JSON_VALUE, "value")
                                            .addCode(body.build())
                                            .build();
                                    break _buildFromValue;
                                }
                            }
                            cls.addMethod(_rBuildFromValue);
                            MethodSpec _rBuildDeserialize;
                            _buildDeserialize:
                            {
                                var body = CodeBlock.builder();
                                body.addStatement(
                                        "$T __reader = new $T(new $T(json))",
                                        CodecEmitter.JSON_READER,
                                        CodecEmitter.JSON_READER,
                                        CodecEmitter.STRING_READER);
                                body.addStatement("return decode(__reader, $T.root())", CodecEmitter.FIELD_PATH);
                                {
                                    _rBuildDeserialize = MethodSpec.methodBuilder("deserialize")
                                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                            .returns(target)
                                            .addParameter(String.class, "json")
                                            .addException(CodecEmitter.IO_EXCEPTION)
                                            .addCode(body.build())
                                            .build();
                                    break _buildDeserialize;
                                }
                            }
                            cls.addMethod(_rBuildDeserialize);
                        }

                        var javaFile = JavaFile.builder(CodecsGen.GEN_PACKAGE, cls.build())
                                .skipJavaLangImports(true)
                                .build();
                        {
                            _rEmit = javaFile.toString();
                            break _emit;
                        }
                    }
                    String src = _rEmit;

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
    }

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
