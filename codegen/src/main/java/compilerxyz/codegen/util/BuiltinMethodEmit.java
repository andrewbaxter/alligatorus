package compilerxyz.codegen.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import compilerxyz.util.IMap;
import compilerxyz.util.Opt;
import compilerxyz.util.Pair;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;

public final class BuiltinMethodEmit {
    private BuiltinMethodEmit() {}

    public record Param(String name, TypeName type) {}

    public record Method(
            String sourceName, String hostMethodName, TList<Param> params, TypeName ret, boolean sourceVisible) {}

    private static final ClassName TORDERED_MAP = ClassName.get("compilerxyz.util", "TOrderedMap");
    private static final ClassName CONST_KEY = ClassName.get("compilerxyz.eval", "ConstKey");
    private static final ClassName CONST_KEY_STR = ClassName.get("compilerxyz.eval", "ConstKey", "Str");
    private static final ClassName RECORD_CLASS = ClassName.get("compilerxyz.eval.value.record", "Record");
    private static final ClassName FUNCTION_PARAM = ClassName.get("compilerxyz.eval.value.function", "FunctionParam");
    private static final ClassName RECORD_FIELD = ClassName.get("compilerxyz.eval.value.record", "RecordField");
    private static final ClassName RECORD_FIELD_DYNAMIC =
            ClassName.get("compilerxyz.eval.value.record", "RecordField", "Dynamic");
    private static final ClassName OPT = ClassName.get("compilerxyz.util", "Opt");
    private static final ClassName TYPE = ClassName.get("compilerxyz.eval", "Type");
    private static final ClassName ILIST = ClassName.get("compilerxyz.util", "IList");
    private static final ClassName TLIST = ClassName.get("compilerxyz.util", "TList");
    private static final ClassName JAVA_METHOD_DESCRIPTOR =
            ClassName.get("compilerxyz.eval.dynamic", "JavaMethodDescriptor");
    private static final ClassName JAVA_DATA_DESCRIPTOR =
            ClassName.get("compilerxyz.eval.dynamic", "JavaDataDescriptor");
    private static final ClassName TYPE_METHOD_DYNAMIC =
            ClassName.get("compilerxyz.eval.value.function", "TypeMethodDynamic");
    private static final ClassName OBJECT_METHOD = ClassName.get("compilerxyz.eval.value.object_", "ObjectMethod");
    private static final ClassName ERESULT = ClassName.get("compilerxyz.eval", "EResult");
    private static final ClassName CODE_VALUE = ClassName.get("compilerxyz.eval", "CodeValue");

    private static final ClassName TYPE_ERESULT_DYNAMIC =
            ClassName.get("compilerxyz.eval.value.eresult", "TypeEResultDynamic");
    private static final ClassName TYPE_LIST_DYNAMIC =
            ClassName.get("compilerxyz.eval.value.list", "TypeListBoxedDynamic");
    private static final ClassName TYPE_OPTION_DYNAMIC =
            ClassName.get("compilerxyz.eval.value.option", "TypeOptionDynamic");
    private static final ClassName TYPE_MAP_DYNAMIC = ClassName.get("compilerxyz.eval.value.map", "TypeMapDynamic");
    private static final ClassName TYPE_BOOL_DYNAMIC = ClassName.get("compilerxyz.eval.value.bool_", "TypeBoolDynamic");
    private static final ClassName TYPE_INT_DYNAMIC = ClassName.get("compilerxyz.eval.value.int_", "TypeIntDynamic");
    private static final ClassName TYPE_LONG_DYNAMIC = ClassName.get("compilerxyz.eval.value.long_", "TypeLongDynamic");
    private static final ClassName TYPE_NULL_DYNAMIC = ClassName.get("compilerxyz.eval.value.null_", "TypeNullDynamic");

    private static final IMap<ClassName, ClassName> COMPOSITE_DESCRIPTORS = IMap.ofEntries(
            new Pair<>(ERESULT, TYPE_ERESULT_DYNAMIC),
            new Pair<>(OPT, TYPE_OPTION_DYNAMIC),
            new Pair<>(TLIST, TYPE_LIST_DYNAMIC),
            new Pair<>(ClassName.get("compilerxyz.util", "TMap"), TYPE_MAP_DYNAMIC));

    public static boolean emitObjectMethod(
            MethodSpec.Builder b,
            ClassName hostClass,
            String receiverVar,
            String methodsVar,
            Method method,
            TMap<ClassName, CodeBlock> leafDescriptors) {
        var retDescOpt = resolveDescriptor(method.ret, leafDescriptors);
        if (retDescOpt.optIsNone()) return false;
        var retDesc = retDescOpt.optUnwrap();
        var paramDescs = new TList<CodeBlock>();
        var paramRawClasses = new TList<TypeName>();
        for (var p : method.params) {
            var pdOpt = resolveDescriptor(p.type, leafDescriptors);
            if (pdOpt.optIsNone()) return false;
            paramDescs.add(pdOpt.optUnwrap());
            var rawOpt = rawClass(p.type);
            if (rawOpt.optIsNone()) return false;
            paramRawClasses.add(rawOpt.optUnwrap());
        }
        var retRawOpt = rawClass(method.ret);
        if (retRawOpt.optIsNone()) return false;
        var retRaw = retRawOpt.optUnwrap();

        var suffix = method.hostMethodName;

        var argRecordName = "params_" + suffix;
        var paramsExpr = CodeBlock.builder();
        paramsExpr.add("$T.<$T>of(", ILIST, FUNCTION_PARAM);
        for (int i = 0; i < method.params.size(); i++) {
            var p = method.params.get(i);
            if (i > 0) paramsExpr.add(", ");
            paramsExpr.add("new $T($S, $L)", FUNCTION_PARAM, p.name, paramDescs.get(i));
        }
        paramsExpr.add(")");
        b.addStatement("var $L = $L", argRecordName, paramsExpr.build());

        var retTypeName = "retType_" + suffix;
        b.addStatement("var $L = $T.<$T>some($L)", retTypeName, OPT, TYPE, retDesc);

        var descName = "desc_" + suffix;
        var argClassBuilder = CodeBlock.builder();
        for (int i = 0; i < paramRawClasses.size(); i++) {
            if (i > 0) argClassBuilder.add(", ");
            argClassBuilder.add("$T.fromClass($T.class)", JAVA_DATA_DESCRIPTOR, paramRawClasses.get(i));
        }
        b.addStatement(
                "var $L = $T.fromParts($T.fromClass($T.class), $T.of($L))",
                descName,
                JAVA_METHOD_DESCRIPTOR,
                JAVA_DATA_DESCRIPTOR,
                retRaw,
                TLIST,
                argClassBuilder.build());

        var jvmClassExpr = CodeBlock.of(
                "$T.fromClass($T.class)", ClassName.get("compilerxyz.eval.dynamic", "JavaInternalName"), hostClass);
        var methodVar = "method_" + suffix;
        b.addStatement(
                "var $L = new $T($N, $L, $L, $L, $S, $L, $T.class.isInterface(), false)",
                methodVar,
                TYPE_METHOD_DYNAMIC,
                receiverVar,
                argRecordName,
                retTypeName,
                jvmClassExpr,
                method.hostMethodName,
                descName,
                hostClass);
        b.addStatement("$L.add(new $T($S, $L))", methodsVar, OBJECT_METHOD, method.sourceName, methodVar);
        return true;
    }

    public static Opt<CodeBlock> resolveDescriptor(TypeName tn, TMap<ClassName, CodeBlock> leafDescriptors) {
        if (tn.equals(TypeName.BOOLEAN)) return Opt.some(CodeBlock.of("new $T()", TYPE_BOOL_DYNAMIC));
        if (tn.equals(TypeName.LONG)) return Opt.some(CodeBlock.of("new $T()", TYPE_LONG_DYNAMIC));
        if (tn.equals(TypeName.VOID)) return Opt.some(CodeBlock.of("new $T()", TYPE_NULL_DYNAMIC));
        if (tn instanceof ClassName cn) {
            if (cn.equals(ClassName.get(String.class))) {
                return Opt.some(
                        CodeBlock.of("new $T()", ClassName.get("compilerxyz.eval.value.string", "TypeStringDynamic")));
            }
            return Opt.ofNullable(leafDescriptors.get(cn));
        }
        if (tn instanceof ParameterizedTypeName pt) {
            var wrapper = COMPOSITE_DESCRIPTORS.getOrNull(pt.rawType);
            if (wrapper == null) return Opt.none();
            var parts = new TList<CodeBlock>();
            for (var arg : pt.typeArguments) {
                var sub = resolveDescriptor(arg, leafDescriptors);
                if (sub.optIsNone()) return Opt.none();
                parts.add(sub.optUnwrap());
            }
            var joined = CodeBlock.builder();
            for (int i = 0; i < parts.size(); i++) {
                if (i > 0) joined.add(", ");
                joined.add(parts.get(i));
            }
            return Opt.some(CodeBlock.of("new $T($L)", wrapper, joined.build()));
        }
        return Opt.none();
    }

    public static Opt<TypeName> rawClass(TypeName tn) {
        if (tn.equals(TypeName.BOOLEAN)) return Opt.some(TypeName.BOOLEAN);
        if (tn.equals(TypeName.LONG)) return Opt.some(TypeName.LONG);
        if (tn.equals(TypeName.VOID)) return Opt.some(TypeName.VOID);
        if (tn instanceof ClassName cn) return Opt.some(cn);
        if (tn instanceof ParameterizedTypeName pt) return Opt.some(pt.rawType);
        return Opt.none();
    }
}
