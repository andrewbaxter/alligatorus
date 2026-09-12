package compilerxyz.codegen;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

public final class OptionsGen {
    private OptionsGen() {}

    public static final ClassName OPTION_SPECIALIZED =
            ClassName.get("compilerxyz.main.eval.value.option", "TypeOptionSpecializedDynamic");
    public static final ClassName OPTION_DYNAMIC = ClassName.get("compilerxyz.main.eval.value.option", "OptionDynamic");
    public static final ClassName OPTION_BOXED =
            ClassName.get("compilerxyz.main.eval.value.option", "TypeOptionDynamic");
    public static final ClassName OPT = ClassName.get("compilerxyz.prelude.util", "Opt");

    public record Store(String simple, TypeName javaPrim, String zeroLiteral) {}

    public record Prim(String name, String storeSimple, ClassName elementClass, long uniqueId) {}

    private static final long FIRST_UNIQUE_ID = 6422L;

    private static String storeSimple(String listStoreSimple) {
        return "OptionStore" + listStoreSimple.substring("ListStore".length());
    }

    public static final IList<Store> STORES;

    public static final IList<Prim> PRIMS;

    static {
        var stores = new TList<Store>();
        for (var s : ListsGen.STORES) {
            String zero;
            if (s.javaPrim().equals(TypeName.BOOLEAN)) zero = "false";
            else if (s.javaPrim().equals(TypeName.LONG)) zero = "0L";
            else if (s.javaPrim().equals(TypeName.FLOAT)) zero = "0f";
            else if (s.javaPrim().equals(TypeName.DOUBLE)) zero = "0d";
            else if (s.javaPrim().equals(TypeName.SHORT)) zero = "(short) 0";
            else if (s.javaPrim().equals(TypeName.BYTE)) zero = "(byte) 0";
            else zero = "0";
            stores.add(new Store(storeSimple(s.simple()), s.javaPrim(), zero));
        }
        STORES = IList.from(stores);
        var prims = new TList<Prim>();
        long id = FIRST_UNIQUE_ID;
        for (var p : ListsGen.PRIMS) {
            prims.add(new Prim(p.name(), storeSimple(p.storeSimple()), p.elementClass(), id));
            id += 1;
        }
        PRIMS = IList.from(prims);
    }

    public static String typeSimple(Prim prim) {
        return "TypeOption" + prim.name() + "Dynamic";
    }
}
