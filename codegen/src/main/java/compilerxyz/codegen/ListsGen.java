package compilerxyz.codegen;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;
import java.util.Locale;

public final class ListsGen {
    private ListsGen() {}

    public static final String GEN_PACKAGE = "compilerxyz.main.gen";
    public static final ClassName TYPE = ClassName.get("compilerxyz.main.eval", "Type");
    public static final ClassName EXPR_CONTEXT = ClassName.get("compilerxyz.main.eval", "ExprContext");
    public static final ClassName JAVA_INTERNAL_NAME =
            ClassName.get("compilerxyz.main.eval.dynamic", "JavaInternalName");
    public static final ClassName LIST_SPECIALIZED =
            ClassName.get("compilerxyz.main.eval.value.list", "TypeListSpecializedDynamic");
    public static final ClassName LIST_DYNAMIC = ClassName.get("compilerxyz.main.eval.value.list", "ListDynamic");
    public static final ClassName LIST_BOXED =
            ClassName.get("compilerxyz.main.eval.value.list", "TypeListBoxedDynamic");
    public static final ClassName GEN_IDENTITIES = ClassName.get(GEN_PACKAGE, "GenTrivialIdentityAspects");

    public record Store(String simple, TypeName javaPrim) {}

    public record Prim(String name, String storeSimple, ClassName elementClass, long uniqueId) {}

    public static final IList<Store> STORES = IList.of(
            new Store("ListStoreInt", TypeName.INT),
            new Store("ListStoreLong", TypeName.LONG),
            new Store("ListStoreShort", TypeName.SHORT),
            new Store("ListStoreByte", TypeName.BYTE),
            new Store("ListStoreFloat", TypeName.FLOAT),
            new Store("ListStoreDouble", TypeName.DOUBLE),
            new Store("ListStoreBool", TypeName.BOOLEAN));

    public static final IList<Prim> PRIMS = IList.of(
            new Prim("Int", "ListStoreInt", ClassName.get("compilerxyz.main.eval.value.int_", "TypeIntDynamic"), 6200L),
            new Prim(
                    "Uint",
                    "ListStoreInt",
                    ClassName.get("compilerxyz.main.eval.value.uint", "TypeUintDynamic"),
                    6201L),
            new Prim(
                    "Long",
                    "ListStoreLong",
                    ClassName.get("compilerxyz.main.eval.value.long_", "TypeLongDynamic"),
                    6202L),
            new Prim(
                    "Ulong",
                    "ListStoreLong",
                    ClassName.get("compilerxyz.main.eval.value.ulong", "TypeUlongDynamic"),
                    6203L),
            new Prim(
                    "Short",
                    "ListStoreShort",
                    ClassName.get("compilerxyz.main.eval.value.short_", "TypeShortDynamic"),
                    6204L),
            new Prim(
                    "Ushort",
                    "ListStoreShort",
                    ClassName.get("compilerxyz.main.eval.value.ushort", "TypeUshortDynamic"),
                    6205L),
            new Prim(
                    "Byte",
                    "ListStoreByte",
                    ClassName.get("compilerxyz.main.eval.value.byte_", "TypeByteDynamic"),
                    6206L),
            new Prim(
                    "Ubyte",
                    "ListStoreByte",
                    ClassName.get("compilerxyz.main.eval.value.ubyte", "TypeUbyteDynamic"),
                    6207L),
            new Prim(
                    "Float",
                    "ListStoreFloat",
                    ClassName.get("compilerxyz.main.eval.value.float_", "TypeFloatDynamic"),
                    6208L),
            new Prim(
                    "Double",
                    "ListStoreDouble",
                    ClassName.get("compilerxyz.main.eval.value.double_", "TypeDoubleDynamic"),
                    6209L),
            new Prim(
                    "Bool",
                    "ListStoreBool",
                    ClassName.get("compilerxyz.main.eval.value.bool", "TypeBoolDynamic"),
                    6210L));

    public static Opt<CodeBlock> specializedListType(ClassName storeClass) {
        if (!storeClass.packageName().equals(GEN_PACKAGE)) return Opt.none();
        for (var prim : PRIMS) {
            if (!prim.storeSimple().equals(storeClass.simpleName())) continue;
            return Opt.some(CodeBlock.of(
                    "new $T(new $T(), $T.fromClass($T.class), $T.$L)",
                    ClassName.get("compilerxyz.main.eval.value.list", "TypeListSpecializedDynamic"),
                    prim.elementClass(),
                    ClassName.get("compilerxyz.main.eval.dynamic", "JavaInternalName"),
                    storeClass,
                    GEN_IDENTITIES,
                    constantName(snake(typeSimple(prim)))));
        }
        return Opt.none();
    }

    public static String typeSimple(Prim prim) {
        return "TypeList" + prim.name() + "Dynamic";
    }

    public static String snake(String camel) {
        var sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            var c = camel.charAt(i);
            if (Character.isUpperCase(c) && i > 0) sb.append('_');
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    public static String constantName(String snake) {
        return snake.toUpperCase(Locale.ROOT) + "_IDENTITY";
    }
}
