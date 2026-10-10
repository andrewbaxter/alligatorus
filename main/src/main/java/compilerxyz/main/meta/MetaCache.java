package compilerxyz.main.meta;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.prelude.util.Opt;

public final class MetaCache {
    public record Preassigned(Metas owner, MetaId id) {
        public void apply(Meta meta) {
            meta.metaAssign(owner, id);
        }
    }

    private final String kind;
    private volatile Opt<Meta> cached = Opt.none();
    private volatile Opt<Preassigned> preassigned = Opt.none();

    public MetaCache(String kind) {
        this.kind = kind;
    }

    public void preassign(Preassigned assignment) {
        preassigned = Opt.some(assignment);
    }

    public Meta get(ExprContext xx, Type type) {
        var current = cached;
        if (current.optIsSome()) return current.optUnwrap();
        var meta = new Meta(kind, Meta.descData(type.typeDesc()));
        cached = Opt.some(meta);
        var assignment = preassigned;
        if (assignment.optIsSome()) assignment.optUnwrap().apply(meta);
        MetaSupport.fillFields(xx, type, meta);
        return meta;
    }
}
