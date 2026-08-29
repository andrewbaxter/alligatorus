package compilerxyz.codegen;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import compilerxyz.codegen.util.NamedTypeRef;
import compilerxyz.codegen.util.PushMethod;
import compilerxyz.codegen.util.TypeRef;
import compilerxyz.prelude.util.ISet;
import compilerxyz.prelude.util.TList;

public final class PushStructsGen {
    private PushStructsGen() {}

    public static final String GEN_PACKAGE = "compilerxyz.main.gen";

    private static final ClassName EVAL_CONTEXT = ClassName.get("compilerxyz.main.eval", "EvalContext");
    public static final ClassName UNIQUE = ClassName.get("compilerxyz.main.eval", "Unique");
    private static final ClassName TYPE_FUNCTION_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.function", "TypeFunctionDynamic");
    private static final ClassName TYPE_METHOD_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.function", "TypeMethodDynamic");
    public static final ClassName JAVA_INTERNAL_NAME =
            ClassName.get("compilerxyz.main.eval.dynamic", "JavaInternalName");
    private static final ClassName JAVA_METHOD_DESCRIPTOR =
            ClassName.get("compilerxyz.main.eval.dynamic", "JavaMethodDescriptor");
    public static final ClassName JAVA_DATA_DESCRIPTOR =
            ClassName.get("compilerxyz.main.eval.dynamic", "JavaDataDescriptor");
    private static final String DYN_RUNTIME_INTNAME = "compilerxyz/main/eval/dynamic/DynRuntime";
    public static final ClassName TYPE_OBJECT_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.object_", "TypeObjectDynamic");
    public static final ClassName ILIST = ClassName.get("compilerxyz.prelude.util", "IList");
    public static final ClassName TYPE_OBJECT_META =
            ClassName.get("compilerxyz.main.eval.value.object_", "TypeObjectMeta");
    public static final ClassName OBJECT_DROP_KIND =
            ClassName.get("compilerxyz.main.eval.value.object_", "ObjectDropKind");
    public static final ClassName TYPE_OBJECT_CAST_RULE =
            ClassName.get("compilerxyz.main.eval.value.object_", "TypeObjectCastRule");
    public static final ClassName TYPE_OBJECT_CAST_FROM_RULE =
            ClassName.get("compilerxyz.main.eval.value.object_", "TypeObjectCastFromRule");
    public static final ClassName OBJECT_DATA_FIELD =
            ClassName.get("compilerxyz.main.eval.value.object_", "ObjectDataField");
    public static final ClassName OBJECT_METHOD = ClassName.get("compilerxyz.main.eval.value.object_", "ObjectMethod");

    public static final ClassName OPT = ClassName.get("compilerxyz.prelude.util", "Opt");
    public static final ClassName TLIST = ClassName.get("compilerxyz.prelude.util", "TList");

    public record PushField(String name, String javaFieldName, TypeRef ret, CodeBlock body) {}

    public record PushStructSpec(
            String name, TList<NamedTypeRef> dataFields, TList<PushField> fields, TList<PushMethod> methods) {}

    public static TypeName rawTypeName(TypeName t) {
        if (t instanceof ParameterizedTypeName p) return p.rawType;
        return t;
    }

    public static final ISet<String> INSTANCE_METHOD_STRUCTS = ISet.of(
            "DefinitionSetBuilder",
            "SourceFuncResult",
            "SourceInterfaceMethodResult",
            "SourceInterfaceResult",
            "SourceObjectBuilder",
            "SourceObjectMethodResult",
            "SourceUnionBuilder");

    public static final ISet<String> HANDWRITTEN_STRUCTS = ISet.of(
            "DefinitionSetBuilder",
            "SourceFuncResult",
            "SourceInterfaceMethodResult",
            "SourceInterfaceResult",
            "SourceObjectBuilder",
            "SourceObjectMethodResult",
            "SourceUnionBuilder");

    public static CodeBlock methodFunctionType(
            PushStructSpec spec, ClassName receiverClass, PushMethod m, String dynRuntimeMethodName) {
        TList<NamedTypeRef> args = m.args();
        CodeBlock _rParamsExpr;
        _paramsExpr:
        {
            var functionParam = ClassName.get("compilerxyz.main.eval.value.function", "FunctionParam");
            var b = CodeBlock.builder();
            b.add("$T.<$T>of(", ILIST, functionParam);
            for (var i = 0; i < args.size(); i++) {
                var a = args.get(i);
                if (i > 0) b.add(", ");
                b.add("new $T($S, $L)", functionParam, a.name(), a.type().dynamicType());
            }
            b.add(")");
            {
                _rParamsExpr = b.build();
                break _paramsExpr;
            }
        }
        CodeBlock argDynType = _rParamsExpr;

        String ret = m.ret();
        CodeBlock _rOptRetType;
        _optRetType:
        {
            if (ret.equals("()") || ret.isEmpty()) {
                {
                    _rOptRetType = CodeBlock.of("$T.none()", OPT);
                    break _optRetType;
                }
            }
            {
                _rOptRetType = CodeBlock.of("$T.some($L)", OPT, ret);
                break _optRetType;
            }
        }
        CodeBlock retDynType = _rOptRetType;

        if (INSTANCE_METHOD_STRUCTS.contains(spec.name())) {
            var receiverInternalName = receiverClass.canonicalName().replace('.', '/');
            CodeBlock _rMethodDescriptorExprVirtual;
            _methodDescriptorExprVirtual:
            {
                var argListExpr = CodeBlock.builder();
                argListExpr.add("$T.<$T>of(", TLIST, JAVA_DATA_DESCRIPTOR);
                var _mdFirst = true;
                if (m.needsEvalContext()) {
                    argListExpr.add("$T.fromClass($T.class)", JAVA_DATA_DESCRIPTOR, EVAL_CONTEXT);
                    _mdFirst = false;
                }
                for (var a : m.args()) {
                    if (!_mdFirst) argListExpr.add(", ");
                    _mdFirst = false;
                    argListExpr.add(
                            "$T.fromClass($T.class)",
                            JAVA_DATA_DESCRIPTOR,
                            rawTypeName(a.type().path()));
                }
                argListExpr.add(")");
                {
                    _rMethodDescriptorExprVirtual = CodeBlock.of(
                            "$T.fromParts($T.fromClass($T.class), $L)",
                            JAVA_METHOD_DESCRIPTOR,
                            JAVA_DATA_DESCRIPTOR,
                            rawTypeName(m.retClass().path()),
                            argListExpr.build());
                    break _methodDescriptorExprVirtual;
                }
            }
            CodeBlock descriptor = _rMethodDescriptorExprVirtual;

            String s = receiverClass.simpleName();
            String _rLowerFirst;
            _lowerFirst:
            {
                var chars = s.toCharArray();
                chars[0] = Character.toLowerCase(chars[0]);
                {
                    _rLowerFirst = new String(chars);
                    break _lowerFirst;
                }
            }
            String receiverField = _rLowerFirst + "_instance";

            String sourceName = m.name();
            String _rJvmMethodName;
            _jvmMethodName:
            {
                var base = pushMethodName(sourceName);
                {
                    _rJvmMethodName = switch (base) {
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
                    break _jvmMethodName;
                }
            }
            return CodeBlock.of(
                    "new $T($N, $L, $L, $T.of($S), $S, $L, false, $L)",
                    TYPE_METHOD_DYNAMIC,
                    receiverField,
                    argDynType,
                    retDynType,
                    JAVA_INTERNAL_NAME,
                    receiverInternalName,
                    _rJvmMethodName,
                    descriptor,
                    m.needsEvalContext());
        }
        CodeBlock _rMethodDescriptorExpr;
        _methodDescriptorExpr:
        {
            var argListExpr = CodeBlock.builder();
            argListExpr.add("$T.of(", TLIST);
            if (m.needsEvalContext()) {
                argListExpr.add("$T.fromClass($T.class), ", JAVA_DATA_DESCRIPTOR, EVAL_CONTEXT);
            }
            argListExpr.add("$T.fromClass($T.class)", JAVA_DATA_DESCRIPTOR, receiverClass);
            for (var a : m.args()) {
                argListExpr.add(
                        ", $T.fromClass($T.class)",
                        JAVA_DATA_DESCRIPTOR,
                        rawTypeName(a.type().path()));
            }
            argListExpr.add(")");
            {
                _rMethodDescriptorExpr = CodeBlock.of(
                        "$T.fromParts($T.fromClass($T.class), $L)",
                        JAVA_METHOD_DESCRIPTOR,
                        JAVA_DATA_DESCRIPTOR,
                        rawTypeName(m.retClass().path()),
                        argListExpr.build());
                break _methodDescriptorExpr;
            }
        }
        CodeBlock descriptor = _rMethodDescriptorExpr;

        return CodeBlock.of(
                "new $T($L, $L, $T.of($S), $S, $L, $L)",
                TYPE_FUNCTION_DYNAMIC,
                argDynType,
                retDynType,
                JAVA_INTERNAL_NAME,
                DYN_RUNTIME_INTNAME,
                dynRuntimeMethodName,
                descriptor,
                m.needsEvalContext());
    }

    public static String pushMethodName(String name) {
        return name.endsWith("_") ? name.substring(0, name.length() - 1) : name;
    }
}
