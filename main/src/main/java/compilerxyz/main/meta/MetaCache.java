package compilerxyz.main.meta;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.prelude.util.Opt;

public final class MetaCache {
    private final String kind;
    private volatile Opt<Meta> cached = Opt.none();

    public MetaCache(String kind) {
        this.kind = kind;
    }

    public Meta get(ExprContext xx, Type type) {
        var current = cached;
        if (current.optIsSome()) return current.optUnwrap();
        var meta = new Meta(kind, Meta.descData(type.typeDesc()));
        cached = Opt.some(meta);
        MetaSupport.fillFields(xx, type, meta);
        return meta;
    }
}
