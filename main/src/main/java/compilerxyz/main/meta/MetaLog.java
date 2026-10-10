package compilerxyz.main.meta;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.ScopeLevel;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TSet;

public final class MetaLog {
    private final Opt<MetaProbe> probe;
    private final TList<QExprId> frames = new TList<>();
    private final TSet<ExprKey> seen = new TSet<>();
    public final TList<Metas.ExprFieldsRow> exprFields = new TList<>();
    public final TList<Metas.ScopeEvent> scopeEvents = new TList<>();

    private record ExprKey(long source, long expr, TList<String> names) {}

    public MetaLog(Opt<MetaProbe> probe) {
        this.probe = probe;
    }

    public void enter(QExprId loc) {
        frames.add(loc);
    }

    public void exit() {
        frames.removeLast();
    }

    public void recordExpr(ExprContext xx, QExprId loc, Value value) {
        if (loc.expr().optIsNone()) return;
        var meta = value.valueType().typeMeta(xx);
        var names = new TList<String>();
        for (var field : MetaSupport.fields(meta)) names.add(field.name());
        if (seen.add(new ExprKey(loc.source().value(), loc.expr().optUnwrap(), names))) {
            exprFields.add(new Metas.ExprFieldsRow(loc, names));
        }
        if (probe.optIsNone() || !probe.optUnwrap().target.equals(loc)) return;
        var scope = new TList<MetaProbe.ScopeEntry>();
        for (var level : xx.scope.levels) {
            switch (level) {
                case ScopeLevel.Entry entry -> {
                    if (entry.name.optIsNone()) continue;
                    scope.add(new MetaProbe.ScopeEntry(
                            MetaSupport.keyName(entry.name.optUnwrap()), entry.binding.bindingMeta(xx)));
                }
                case ScopeLevel.Temp t -> {}
                case ScopeLevel.Labeled l -> {}
                case ScopeLevel.Catch c -> {}
                case ScopeLevel.Finally f -> {}
            }
        }
        probe.optUnwrap().hits.add(new MetaProbe.Hit(xx, meta, scope));
    }

    public void scopeBegin(QExprId loc) {
        scopeEvents.add(new Metas.ScopeEvent(scopeEvents.size(), loc, Metas.ScopeEventKind.BEGIN, ""));
    }

    public void scopeEnd(QExprId loc) {
        scopeEvents.add(new Metas.ScopeEvent(scopeEvents.size(), loc, Metas.ScopeEventKind.END, ""));
    }

    public void scopeEnter(QExprId loc) {
        if (loc.expr().optIsNone()) return;
        scopeEvents.add(new Metas.ScopeEvent(scopeEvents.size(), loc, Metas.ScopeEventKind.ENTER, ""));
    }

    public void scopePush(Opt<ConstKey> name) {
        scopeEvent(Metas.ScopeEventKind.PUSH, name);
    }

    public void scopeSet(Opt<ConstKey> name) {
        scopeEvent(Metas.ScopeEventKind.SET, name);
    }

    public void scopePop(Opt<ConstKey> name) {
        scopeEvent(Metas.ScopeEventKind.POP, name);
    }

    private void scopeEvent(Metas.ScopeEventKind kind, Opt<ConstKey> name) {
        if (name.optIsNone()) return;
        if (frames.isEmpty()) return;
        scopeEvents.add(new Metas.ScopeEvent(
                scopeEvents.size(), frames.get(frames.size() - 1), kind, MetaSupport.keyName(name.optUnwrap())));
    }
}
