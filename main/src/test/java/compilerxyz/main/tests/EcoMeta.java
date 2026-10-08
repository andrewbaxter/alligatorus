package compilerxyz.main.tests;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

public final class EcoMeta {
    private EcoMeta() {}

    public static final String FILE = "meta.atz";

    private static final IList<String> VARIANTS = IList.of("str", "int", "float", "bool", "list", "map");

    private static SourceExpr valueT(EcoMb e) {
        return e.fld(e.v("value_u"), "type");
    }

    public static TList<SourceExpr> module(EcoMb e) {
        var top = new TList<SourceExpr>();
        top.add(e.let("d", e.define()));
        top.add(e.let("value_u", e.callM0(e.v("d"), "union")));
        for (var tag : VARIANTS) {
            var typeRef =
                    switch (tag) {
                        case "str" -> e.mb.typesString();
                        case "int" -> e.mb.typesInt();
                        case "float" -> e.typesDouble();
                        case "bool" -> e.mb.typesBool();
                        case "list" -> e.listType(valueT(e));
                        case "map" -> e.mapType(e.mb.typesString(), valueT(e));
                        default -> throw new IllegalArgumentException(tag);
                    };
            top.add(e.let(
                    "v_" + tag,
                    e.callM(e.v("value_u"), "variant", e.rec(e.p("tag", e.s(tag)), e.p("type_ref", typeRef)))));
        }
        top.add(e.finish(e.v("d")));
        var defs = new TList<EcoMb.P>();
        defs.add(e.p("value_t", valueT(e)));
        defs.add(e.p("other_t", e.rec(e.p("type", e.mb.typesString()), e.p("value", valueT(e)))));
        for (var tag : VARIANTS) defs.add(e.p(tag, e.v("v_" + tag)));
        top.add(e.ret(e.rec(defs.toArray(EcoMb.P[]::new))));
        return top;
    }
}
