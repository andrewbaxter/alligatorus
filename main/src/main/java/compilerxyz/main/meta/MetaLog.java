package compilerxyz.main.meta;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TSet;
import java.util.IdentityHashMap;
import java.util.Map;

public final class MetaLog {
    private final Metas metas;
    private final TList<Frame> frames = new TList<>();
    private final TSet<ExprKey> seen = new TSet<>();
    private final Map<Type, Meta> byType = new IdentityHashMap<>();
    public final TList<Metas.ExprMetaRow> exprMetas = new TList<>();
    public final TList<Metas.ScopeEvent> scopeEvents = new TList<>();

    private record Frame(ExprContext xx, QExprId loc) {}

    private record ExprKey(long source, long expr, long meta) {}

    public MetaLog(Metas metas) {
        this.metas = metas;
    }

    public void enter(ExprContext xx, QExprId loc) {
        frames.add(new Frame(xx, loc));
    }

    public void exit() {
        frames.removeLast();
    }

    public Meta metaOf(ExprContext xx, Type type) {
        var cached = byType.get(type);
        if (cached != null) return cached;
        var meta = type.typeMeta(xx);
        byType.put(type, meta);
        return meta;
    }

    public void recordExpr(ExprContext xx, QExprId loc, Value value) {
        if (loc.expr().optIsNone()) return;
        var meta = metaOf(xx, value.valueType());
        var id = metas.metasIntern(xx, meta);
        if (!seen.add(new ExprKey(loc.source().value(), loc.expr().optUnwrap(), id.value()))) return;
        exprMetas.add(new Metas.ExprMetaRow(loc, id));
    }

    public void scopeBegin(QExprId loc) {
        scopeEvents.add(new Metas.ScopeEvent(scopeEvents.size(), loc, Metas.ScopeEventKind.BEGIN, "", Opt.none()));
    }

    public void scopeEnd(QExprId loc) {
        scopeEvents.add(new Metas.ScopeEvent(scopeEvents.size(), loc, Metas.ScopeEventKind.END, "", Opt.none()));
    }

    public void scopeEnter(QExprId loc) {
        if (loc.expr().optIsNone()) return;
        scopeEvents.add(new Metas.ScopeEvent(scopeEvents.size(), loc, Metas.ScopeEventKind.ENTER, "", Opt.none()));
    }

    public void scopePush(Opt<ConstKey> name, Binding binding) {
        scopeEvent(Metas.ScopeEventKind.PUSH, name, Opt.some(binding));
    }

    public void scopeSet(Opt<ConstKey> name, Binding binding) {
        scopeEvent(Metas.ScopeEventKind.SET, name, Opt.some(binding));
    }

    public void scopePop(Opt<ConstKey> name) {
        scopeEvent(Metas.ScopeEventKind.POP, name, Opt.none());
    }

    private void scopeEvent(Metas.ScopeEventKind kind, Opt<ConstKey> name, Opt<Binding> binding) {
        if (name.optIsNone()) return;
        if (frames.isEmpty()) return;
        var frame = frames.get(frames.size() - 1);
        Opt<MetaId> meta = Opt.none();
        if (binding.optIsSome()) {
            meta = Opt.some(metas.metasIntern(frame.xx(), binding.optUnwrap().bindingMeta(frame.xx())));
        }
        scopeEvents.add(new Metas.ScopeEvent(
                scopeEvents.size(), frame.loc(), kind, MetaSupport.keyName(name.optUnwrap()), meta));
    }
}
