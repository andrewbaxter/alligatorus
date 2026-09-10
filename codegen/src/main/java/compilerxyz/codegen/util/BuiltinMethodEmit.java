package compilerxyz.codegen.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import compilerxyz.prelude.util.IMap;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Pair;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;

public final class BuiltinMethodEmit {
    private BuiltinMethodEmit() {}

    public record Param(String name, TypeName type) {}

    public record Method(
            String sourceName,
            String hostMethodName,
            TList<Param> params,
            TypeName ret,
            boolean sourceVisible,
            boolean needsEvalContext) {}

    private static final ClassName TORDERED_MAP = ClassName.get("compilerxyz.prelude.util", "TOrderedMap");
    public static final ClassName FUNCTION_PARAM =
            ClassName.get("compilerxyz.main.eval.value.function", "FunctionParam");
    public static final ClassName OPT = ClassName.get("compilerxyz.prelude.util", "Opt");
    public static final ClassName TYPE = ClassName.get("compilerxyz.main.eval", "Type");
    public static final ClassName ILIST = ClassName.get("compilerxyz.prelude.util", "IList");
    public static final ClassName TLIST = ClassName.get("compilerxyz.prelude.util", "TList");
    public static final ClassName JAVA_METHOD_DESCRIPTOR =
            ClassName.get("compilerxyz.main.eval.dynamic", "JavaMethodDescriptor");
    public static final ClassName JAVA_DATA_DESCRIPTOR =
            ClassName.get("compilerxyz.main.eval.dynamic", "JavaDataDescriptor");
    public static final ClassName TYPE_METHOD_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.function", "TypeMethodDynamic");
    public static final ClassName OBJECT_METHOD = ClassName.get("compilerxyz.main.eval.value.object", "ObjectMethod");
    private static final ClassName ERESULT = ClassName.get("compilerxyz.main.eval", "EResult");

    private static final ClassName TYPE_ERESULT_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.eresult", "TypeEResultDynamic");
    private static final ClassName TYPE_LIST_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.list", "TypeListBoxedDynamic");
    private static final ClassName TYPE_OPTION_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.option", "TypeOptionDynamic");
    private static final ClassName TYPE_MAP_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.map", "TypeMapDynamic");
    private static final ClassName TYPE_BOOL_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.bool", "TypeBoolDynamic");
    private static final ClassName TYPE_LONG_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.long_", "TypeLongDynamic");
    private static final ClassName TYPE_DOUBLE_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.double_", "TypeDoubleDynamic");
    private static final ClassName TYPE_NULL_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.null_", "TypeNullDynamic");

    private static final IMap<ClassName, ClassName> COMPOSITE_DESCRIPTORS = IMap.ofEntries(
            new Pair<>(ERESULT, TYPE_ERESULT_DYNAMIC),
            new Pair<>(OPT, TYPE_OPTION_DYNAMIC),
            new Pair<>(TLIST, TYPE_LIST_DYNAMIC),
            new Pair<>(ClassName.get("compilerxyz.prelude.util", "TMap"), TYPE_MAP_DYNAMIC),
            new Pair<>(TORDERED_MAP, TYPE_MAP_DYNAMIC));

    private static ClassName valueType(String pkg, String cls) {
        return ClassName.get("compilerxyz.main.eval.value." + pkg, cls);
    }

    public static Opt<CodeBlock> resolveDescriptor(TypeName tn, TMap<ClassName, CodeBlock> leafDescriptors) {
        if (tn.equals(TypeName.BOOLEAN)) return Opt.some(CodeBlock.of("new $T()", TYPE_BOOL_DYNAMIC));
        if (tn.equals(TypeName.LONG)) return Opt.some(CodeBlock.of("new $T()", TYPE_LONG_DYNAMIC));
        if (tn.equals(TypeName.DOUBLE)) return Opt.some(CodeBlock.of("new $T()", TYPE_DOUBLE_DYNAMIC));
        if (tn.equals(TypeName.VOID)) return Opt.some(CodeBlock.of("new $T()", TYPE_NULL_DYNAMIC));
        if (tn.equals(TypeName.INT)) return Opt.some(CodeBlock.of("new $T()", valueType("int_", "TypeIntDynamic")));
        if (tn.equals(TypeName.BYTE)) return Opt.some(CodeBlock.of("new $T()", valueType("byte_", "TypeByteDynamic")));
        if (tn.equals(TypeName.SHORT)) {
            return Opt.some(CodeBlock.of("new $T()", valueType("short_", "TypeShortDynamic")));
        }
        if (tn.equals(TypeName.FLOAT)) {
            return Opt.some(CodeBlock.of("new $T()", valueType("float_", "TypeFloatDynamic")));
        }
        if (tn instanceof ClassName cn) {
            if (cn.equals(ClassName.get(Integer.class))) {
                return Opt.some(CodeBlock.of("new $T()", valueType("int_", "TypeIntDynamicBox")));
            }
            if (cn.equals(ClassName.get(Long.class))) {
                return Opt.some(CodeBlock.of("new $T()", valueType("long_", "TypeLongDynamicBox")));
            }
            if (cn.equals(ClassName.get(Double.class))) {
                return Opt.some(CodeBlock.of("new $T()", valueType("double_", "TypeDoubleDynamicBox")));
            }
            if (cn.equals(ClassName.get(Void.class))
                    || cn.equals(ClassName.get(Number.class))
                    || cn.equals(ClassName.get(java.util.Iterator.class))) {
                return Opt.some(CodeBlock.of("new $T()", TYPE_NULL_DYNAMIC));
            }
            if (cn.equals(ClassName.get(String.class))) {
                return Opt.some(CodeBlock.of(
                        "new $T()", ClassName.get("compilerxyz.main.eval.value.string", "TypeStringDynamic")));
            }
            if (cn.equals(ClassName.get(Boolean.class))) {
                return Opt.some(CodeBlock.of("new $T()", TYPE_BOOL_DYNAMIC));
            }
            if (cn.equals(ClassName.get(Object.class))) {
                return Opt.some(CodeBlock.of("new $T()", TYPE_NULL_DYNAMIC));
            }
            if (cn.equals(ClassName.get("compilerxyz.main.eval.expr", "StagedExprCell"))) {
                return Opt.some(
                        CodeBlock.of("new $T()", ClassName.get("compilerxyz.main.eval.value.expr", "TypeExpr")));
            }
            return Opt.ofNullable(leafDescriptors.get(cn));
        }
        if (tn instanceof ParameterizedTypeName pt) {
            if (pt.rawType.equals(ClassName.get(java.util.Iterator.class))) {
                return Opt.some(CodeBlock.of("new $T()", TYPE_NULL_DYNAMIC));
            }
            var wrapper = COMPOSITE_DESCRIPTORS.getOrNull(pt.rawType);
            if (wrapper == null) return Opt.ofNullable(leafDescriptors.get(pt.rawType));
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
        if (tn.equals(TypeName.INT)) return Opt.some(TypeName.INT);
        if (tn.equals(TypeName.BYTE)) return Opt.some(TypeName.BYTE);
        if (tn.equals(TypeName.SHORT)) return Opt.some(TypeName.SHORT);
        if (tn.equals(TypeName.FLOAT)) return Opt.some(TypeName.FLOAT);
        if (tn.equals(TypeName.BOOLEAN)) return Opt.some(TypeName.BOOLEAN);
        if (tn.equals(TypeName.LONG)) return Opt.some(TypeName.LONG);
        if (tn.equals(TypeName.DOUBLE)) return Opt.some(TypeName.DOUBLE);
        if (tn.equals(TypeName.VOID)) return Opt.some(TypeName.VOID);
        if (tn instanceof ClassName cn) return Opt.some(cn);
        if (tn instanceof ParameterizedTypeName pt) return Opt.some(pt.rawType);
        return Opt.none();
    }
}
