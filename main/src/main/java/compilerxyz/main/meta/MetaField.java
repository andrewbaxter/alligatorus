package compilerxyz.main.meta;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.Opt;
import java.util.function.Function;

public record MetaField(JsonValue data, Opt<Function<ExprContext, Meta>> meta) {
    public static MetaField of(Meta meta) {
        return new MetaField(Meta.emptyData(), Opt.some(xx -> meta));
    }

    public static MetaField ofLazy(Function<ExprContext, Meta> meta) {
        return new MetaField(Meta.emptyData(), Opt.some(meta));
    }

    public static MetaField dataOnly(JsonValue data) {
        return new MetaField(data, Opt.none());
    }
}
