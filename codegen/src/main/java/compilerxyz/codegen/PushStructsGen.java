package compilerxyz.codegen;

import com.palantir.javaformat.java.Formatter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import compilerxyz.codegen.util.NamedTypeRef;
import compilerxyz.codegen.util.PushMethod;
import compilerxyz.codegen.util.TypeRef;
import compilerxyz.prelude.util.ISet;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TSet;
import java.io.IOException;
import java.nio.file.Path;
import javax.lang.model.element.Modifier;

public final class PushStructsGen {
    private PushStructsGen() {}

    private static final String GEN_PACKAGE = "compilerxyz.main.gen";

    private static final ClassName TYPE = ClassName.get("compilerxyz.main.eval", "Type");
    private static final ClassName UNIQUE = ClassName.get("compilerxyz.main.eval", "Unique");
    private static final ClassName EXPR_CONTEXT = ClassName.get("compilerxyz.main.eval", "ExprContext");
    private static final ClassName TYPE_FUNCTION_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.function", "TypeFunctionDynamic");
    private static final ClassName TYPE_METHOD_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.function", "TypeMethodDynamic");
    private static final ClassName JAVA_INTERNAL_NAME =
            ClassName.get("compilerxyz.main.eval.dynamic", "JavaInternalName");
    private static final ClassName JAVA_METHOD_DESCRIPTOR =
            ClassName.get("compilerxyz.main.eval.dynamic", "JavaMethodDescriptor");
    private static final ClassName JAVA_DATA_DESCRIPTOR =
            ClassName.get("compilerxyz.main.eval.dynamic", "JavaDataDescriptor");
    private static final String DYN_RUNTIME_INTNAME = "compilerxyz/main/eval/dynamic/DynRuntime";
    private static final ClassName TYPE_OBJECT_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.object_", "TypeObjectDynamic");
    private static final ClassName ILIST = ClassName.get("compilerxyz.prelude.util", "IList");
    private static final ClassName TYPE_OBJECT_META =
            ClassName.get("compilerxyz.main.eval.value.object_", "TypeObjectMeta");
    private static final ClassName OBJECT_DROP_KIND =
            ClassName.get("compilerxyz.main.eval.value.object_", "ObjectDropKind");
    private static final ClassName TYPE_OBJECT_CAST_RULE =
            ClassName.get("compilerxyz.main.eval.value.object_", "TypeObjectCastRule");
    private static final ClassName TYPE_OBJECT_CAST_FROM_RULE =
            ClassName.get("compilerxyz.main.eval.value.object_", "TypeObjectCastFromRule");
    private static final ClassName OBJECT_DATA_FIELD =
            ClassName.get("compilerxyz.main.eval.value.object_", "ObjectDataField");
    private static final ClassName OBJECT_METHOD = ClassName.get("compilerxyz.main.eval.value.object_", "ObjectMethod");

    private static final ClassName OPT = ClassName.get("compilerxyz.prelude.util", "Opt");
    private static final ClassName TLIST = ClassName.get("compilerxyz.prelude.util", "TList");

    public record PushField(String name, String javaFieldName, TypeRef ret, CodeBlock body) {}

    public record PushStructSpec(
            String name, TList<NamedTypeRef> dataFields, TList<PushField> fields, TList<PushMethod> methods) {}

    public static void run(
            Path outRoot, Formatter formatter, TList<PushStructSpec> specs, TList<PushStructSpec> extraSpecs)
            throws IOException {
        var all = new TList<PushStructSpec>();
        all.addAll(specs);
        all.addAll(extraSpecs);
        validate(all);
        for (var spec : all) {
            if (HANDWRITTEN_STRUCTS.contains(spec.name())) continue;
            writeDataClass(outRoot, formatter, spec);
        }
        writeGenObjectTypes(outRoot, formatter, all);
    }

    private static void validate(TList<PushStructSpec> specs) {
        var names = new TSet<String>();
        for (var spec : specs) {
            if (!names.add(spec.name())) {
                throw new IllegalStateException("duplicate push struct name: " + spec.name());
            }
            var memberNames = new TSet<String>();
            for (var f : spec.fields()) {
                if (!memberNames.add(pushMethodName(f.name()))) {
                    throw new IllegalStateException("duplicate field/method name on " + spec.name() + ": " + f.name());
                }
            }
            for (var m : spec.methods()) {
                if (!memberNames.add(pushMethodName(m.name()))) {
                    throw new IllegalStateException("duplicate field/method name on " + spec.name() + ": " + m.name());
                }
            }
        }
    }

    private static void writeDataClass(Path outRoot, Formatter formatter, PushStructSpec spec) throws IOException {
        var chars = spec.name().toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        var factoryName = new String(chars);
        var genObjectTypes = ClassName.get(GEN_PACKAGE, "GenObjectTypes");
        var cls = TypeSpec.classBuilder(spec.name()).addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        var ctor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
        for (var df : spec.dataFields()) {
            cls.addField(FieldSpec.builder(df.type().path(), df.name(), Modifier.PUBLIC, Modifier.FINAL)
                    .build());
            if (df.derivedFrom().optIsSome()) {
                ctor.addStatement("this.$N = $L", df.name(), df.derivedFrom().optUnwrap());
            } else {
                ctor.addParameter(
                        ParameterSpec.builder(df.type().path(), df.name()).build());
                ctor.addStatement("this.$N = $N", df.name(), df.name());
            }
        }
        cls.addMethod(ctor.build());

        cls.addMethod(MethodSpec.methodBuilder("dynamicType")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TYPE_OBJECT_DYNAMIC)
                .addStatement("return $T.$L()", genObjectTypes, factoryName)
                .build());

        var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
        GenMain.write(outRoot, file, formatter);
    }

    private static void writeGenObjectTypes(Path outRoot, Formatter formatter, TList<PushStructSpec> specs)
            throws IOException {
        var cls = TypeSpec.classBuilder("GenObjectTypes").addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        cls.addMethod(
                MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
        for (var spec : specs) {
            var chars = spec.name().toCharArray();
            chars[0] = Character.toLowerCase(chars[0]);
            var fieldName = new String(chars) + "_instance";
            cls.addField(FieldSpec.builder(TYPE_OBJECT_DYNAMIC, fieldName, Modifier.PRIVATE, Modifier.STATIC)
                    .build());
            cls.addMethod(buildObjectTypeFactory(spec, fieldName));
        }
        var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
        GenMain.write(outRoot, file, formatter);
    }

    private static MethodSpec buildObjectTypeFactory(PushStructSpec spec, String fieldName) {
        var chars = spec.name().toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        var factoryName = new String(chars);
        var specClass = ClassName.get(GEN_PACKAGE, spec.name());
        var jvmInternal = specClass.canonicalName().replace('.', '/');

        var dataFieldsList = CodeBlock.builder();
        dataFieldsList.add("$T.<$T>of(", TLIST, OBJECT_DATA_FIELD);
        for (int i = 0; i < spec.fields().size(); i++) {
            var f = spec.fields().get(i);
            if (i > 0) dataFieldsList.add(", ");
            dataFieldsList.add(
                    "$T.ofField($S, $S, $T.fromClass($T.class), $L)",
                    OBJECT_DATA_FIELD,
                    pushMethodName(f.name()),
                    f.javaFieldName(),
                    JAVA_DATA_DESCRIPTOR,
                    rawTypeName(f.ret().path()),
                    f.ret().dynamicType());
        }
        dataFieldsList.add(")");

        var isSelfReferential = INSTANCE_METHOD_STRUCTS.contains(spec.name());
        var methodsList = CodeBlock.builder();
        if (isSelfReferential) {
            methodsList.add("$N", "__methods");
        } else {
            methodsList.add("$T.<$T>of(", TLIST, OBJECT_METHOD);
            for (int i = 0; i < spec.methods().size(); i++) {
                var meth = spec.methods().get(i);
                var dynRuntimeMethod = meth.dynRuntimeMethod();
                if (dynRuntimeMethod == null && !INSTANCE_METHOD_STRUCTS.contains(spec.name())) {
                    throw new IllegalStateException("PushMethod '" + meth.name() + "' has no dynRuntimeMethod set");
                }
                if (i > 0) methodsList.add(", ");
                methodsList.add(
                        "new $T($S, $L)",
                        OBJECT_METHOD,
                        pushMethodName(meth.name()),
                        methodFunctionType(spec, specClass, meth, dynRuntimeMethod));
            }
            methodsList.add(")");
        }

        var builder = MethodSpec.methodBuilder(factoryName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TYPE_OBJECT_DYNAMIC)
                .beginControlFlow("if ($N == null)", fieldName);
        if (isSelfReferential) {
            builder.addStatement("var __methods = new $T<$T>()", TLIST, OBJECT_METHOD);
        }
        builder.addStatement(
                "var __meta = new $T($S, $T.<$T>of(), $T.<$T>of(), $T.<$T>of(), $T.none())",
                TYPE_OBJECT_META,
                spec.name(),
                ILIST,
                UNIQUE,
                ILIST,
                TYPE_OBJECT_CAST_RULE,
                ILIST,
                TYPE_OBJECT_CAST_FROM_RULE,
                OPT);
        builder.addStatement(
                "$N = new $T(__meta, $T.of($S), $T.fromClass($T.class), $T.class, " + "$T.DYNAMIC, $T.none())",
                fieldName,
                TYPE_OBJECT_DYNAMIC,
                JAVA_INTERNAL_NAME,
                jvmInternal,
                JAVA_DATA_DESCRIPTOR,
                specClass,
                specClass,
                OBJECT_DROP_KIND,
                OPT);
        if (!isSelfReferential) {
            builder.addStatement(
                    "$N.linkInit($T.from($L), $T.from($L))",
                    fieldName,
                    ILIST,
                    dataFieldsList.build(),
                    ILIST,
                    methodsList.build());
        }
        if (isSelfReferential) {
            for (var meth : spec.methods()) {
                var dynRuntimeMethod = meth.dynRuntimeMethod();
                if (dynRuntimeMethod == null && !INSTANCE_METHOD_STRUCTS.contains(spec.name())) {
                    throw new IllegalStateException("PushMethod '" + meth.name() + "' has no dynRuntimeMethod set");
                }
                builder.addStatement(
                        "__methods.add(new $T($S, $L))",
                        OBJECT_METHOD,
                        pushMethodName(meth.name()),
                        methodFunctionType(spec, specClass, meth, dynRuntimeMethod));
            }
            builder.addStatement(
                    "$N.linkInit($T.from($L), $T.from(__methods))", fieldName, ILIST, dataFieldsList.build(), ILIST);
        }
        return builder.endControlFlow().addStatement("return $N", fieldName).build();
    }

    private static TypeName rawTypeName(TypeName t) {
        if (t instanceof ParameterizedTypeName p) return p.rawType;
        return t;
    }

    private static final ISet<String> INSTANCE_METHOD_STRUCTS = ISet.of(
            "DefinitionSetBuilder",
            "SourceFuncResult",
            "SourceInterfaceMethodResult",
            "SourceInterfaceResult",
            "SourceObjectBuilder",
            "SourceObjectMethodResult",
            "SourceUnionBuilder");

    private static final ISet<String> HANDWRITTEN_STRUCTS = ISet.of(
            "DefinitionSetBuilder",
            "SourceFuncResult",
            "SourceInterfaceMethodResult",
            "SourceInterfaceResult",
            "SourceObjectBuilder",
            "SourceObjectMethodResult",
            "SourceUnionBuilder");

    private static CodeBlock methodFunctionType(
            PushStructSpec spec, ClassName receiverClass, PushMethod m, String dynRuntimeMethodName) {
        var argDynType = paramsExpr(m.args());
        var retDynType = optRetType(m.ret());
        if (INSTANCE_METHOD_STRUCTS.contains(spec.name())) {
            var receiverInternalName = receiverClass.canonicalName().replace('.', '/');
            var descriptor = methodDescriptorExprVirtual(receiverClass, m);
            var receiverField = lowerFirst(receiverClass.simpleName()) + "_instance";
            return CodeBlock.of(
                    "new $T($N, $L, $L, $T.of($S), $S, $L, false, true)",
                    TYPE_METHOD_DYNAMIC,
                    receiverField,
                    argDynType,
                    retDynType,
                    JAVA_INTERNAL_NAME,
                    receiverInternalName,
                    jvmMethodName(m.name()),
                    descriptor);
        }
        var descriptor = methodDescriptorExpr(receiverClass, m);
        return CodeBlock.of(
                "new $T($L, $L, $T.of($S), $S, $L, true)",
                TYPE_FUNCTION_DYNAMIC,
                argDynType,
                retDynType,
                JAVA_INTERNAL_NAME,
                DYN_RUNTIME_INTNAME,
                dynRuntimeMethodName,
                descriptor);
    }

    private static String lowerFirst(String s) {
        var chars = s.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    private static CodeBlock methodDescriptorExpr(ClassName receiverClass, PushMethod m) {
        var argListExpr = CodeBlock.builder();
        argListExpr.add("$T.of($T.fromClass($T.class)", TLIST, JAVA_DATA_DESCRIPTOR, EXPR_CONTEXT);
        argListExpr.add(", $T.fromClass($T.class)", JAVA_DATA_DESCRIPTOR, receiverClass);
        for (var a : m.args()) {
            argListExpr.add(
                    ", $T.fromClass($T.class)",
                    JAVA_DATA_DESCRIPTOR,
                    rawTypeName(a.type().path()));
        }
        argListExpr.add(")");
        return CodeBlock.of(
                "$T.fromParts($T.fromClass($T.class), $L)",
                JAVA_METHOD_DESCRIPTOR,
                JAVA_DATA_DESCRIPTOR,
                rawTypeName(m.retClass().path()),
                argListExpr.build());
    }

    private static CodeBlock methodDescriptorExprVirtual(ClassName receiverClass, PushMethod m) {
        var argListExpr = CodeBlock.builder();
        argListExpr.add("$T.of($T.fromClass($T.class)", TLIST, JAVA_DATA_DESCRIPTOR, EXPR_CONTEXT);
        for (var a : m.args()) {
            argListExpr.add(
                    ", $T.fromClass($T.class)",
                    JAVA_DATA_DESCRIPTOR,
                    rawTypeName(a.type().path()));
        }
        argListExpr.add(")");
        return CodeBlock.of(
                "$T.fromParts($T.fromClass($T.class), $L)",
                JAVA_METHOD_DESCRIPTOR,
                JAVA_DATA_DESCRIPTOR,
                rawTypeName(m.retClass().path()),
                argListExpr.build());
    }

    private static CodeBlock paramsExpr(TList<NamedTypeRef> args) {
        var functionParam = ClassName.get("compilerxyz.main.eval.value.function", "FunctionParam");
        var b = CodeBlock.builder();
        b.add("$T.<$T>of(", ILIST, functionParam);
        for (var i = 0; i < args.size(); i++) {
            var a = args.get(i);
            if (i > 0) b.add(", ");
            b.add("new $T($S, $L)", functionParam, a.name(), a.type().dynamicType());
        }
        b.add(")");
        return b.build();
    }

    private static CodeBlock optRetType(String ret) {
        if (ret.equals("()") || ret.isEmpty()) {
            return CodeBlock.of("$T.none()", OPT);
        }
        return CodeBlock.of("$T.some($L)", OPT, ret);
    }

    public static TList<PushStructSpec> specs() {
        return TList.of();
    }

    private static String pushMethodName(String name) {
        return name.endsWith("_") ? name.substring(0, name.length() - 1) : name;
    }

    private static String jvmMethodName(String sourceName) {
        var base = pushMethodName(sourceName);
        return switch (base) {
            case "interface",
                    "new",
                    "class",
                    "return",
                    "if",
                    "else",
                    "for",
                    "while",
                    "switch",
                    "case",
                    "default",
                    "break",
                    "continue",
                    "true",
                    "false",
                    "null",
                    "void",
                    "int",
                    "long",
                    "boolean",
                    "float",
                    "double",
                    "char",
                    "byte",
                    "short",
                    "this",
                    "super",
                    "static",
                    "final",
                    "public",
                    "private",
                    "protected",
                    "abstract",
                    "synchronized",
                    "volatile",
                    "transient",
                    "native",
                    "throws",
                    "throw",
                    "try",
                    "catch",
                    "finally",
                    "import",
                    "package",
                    "extends",
                    "implements",
                    "instanceof",
                    "do",
                    "goto",
                    "const",
                    "enum" -> base + "_";
            default -> base;
        };
    }
}
