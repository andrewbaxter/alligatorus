package compilerxyz.main.meta;

import compilerxyz.main.eval.SourceId;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.ArrayList;

public final class ScopeReplay {
    private ScopeReplay() {}

    public record Entry(String name, Opt<MetaId> meta) {}

    public static Opt<Long> enclosingStatement(SourceModule module, long expr) {
        var roots =
                switch (module) {
                    case SourceModule.V1 v1 -> v1.exprs();
                };
        for (var root : roots) {
            var found = find(root, expr, root.id().value());
            if (found.optIsSome()) return found;
        }
        return Opt.none();
    }

    private static Opt<Long> find(SourceExpr expr, long target, long statement) {
        if (expr.id().value() == target) return Opt.some(statement);
        var isSeq =
                switch (expr.variant()) {
                    case SourceExpr.Seq s -> true;
                    default -> false;
                };
        for (var child : children(expr)) {
            var found = find(child, target, isSeq ? child.id().value() : statement);
            if (found.optIsSome()) return found;
        }
        return Opt.none();
    }

    public static TList<SourceExpr> children(SourceExpr expr) {
        var out = new TList<SourceExpr>();
        switch (expr.variant()) {
            case SourceExpr.As e -> {
                out.add(e.base());
                out.add(e.target());
            }
            case SourceExpr.Bind e -> {
                out.add(e.name());
                out.add(e.value());
            }
            case SourceExpr.Call e -> {
                out.add(e.func());
                out.add(e.arg());
            }
            case SourceExpr.Field e -> {
                out.add(e.base());
                out.add(e.key());
            }
            case SourceExpr.Foreach e -> {
                out.add(e.base());
                out.add(e.elementKey());
                out.add(e.body());
            }
            case SourceExpr.Graft e -> out.add(e.expr());
            case SourceExpr.If e -> {
                for (var branch : e.conditions()) {
                    out.add(branch.condition());
                    out.add(branch.body());
                }
                if (e.fallback().optIsSome()) out.add(e.fallback().optUnwrap());
            }
            case SourceExpr.Labeled e -> out.add(e.inner());
            case SourceExpr.Literal e -> {}
            case SourceExpr.Lower e -> out.add(e.expr());
            case SourceExpr.Match e -> {
                out.add(e.base());
                out.add(e.elementKey());
                for (var branch : e.cases()) {
                    out.add(branch.condition());
                    out.add(branch.body());
                }
                if (e.fallback().optIsSome()) out.add(e.fallback().optUnwrap());
            }
            case SourceExpr.OperatorBinary e -> {
                out.add(e.base());
                out.add(e.reference());
            }
            case SourceExpr.OperatorUnary e -> out.add(e.base());
            case SourceExpr.Record e -> {
                for (var entry : e.entries()) {
                    out.add(entry.key());
                    out.add(entry.value());
                }
            }
            case SourceExpr.Return e -> {
                if (e.value().optIsSome()) out.add(e.value().optUnwrap());
            }
            case SourceExpr.ScopeUse e -> out.add(e.key());
            case SourceExpr.Seq e -> out.addAll(e.exprs());
            case SourceExpr.Set e -> {
                out.add(e.base());
                out.add(e.value());
            }
            case SourceExpr.Stage e -> out.add(e.expr());
            case SourceExpr.Try e -> {
                out.add(e.body());
                if (e.catchClause().optIsSome()) {
                    var c = e.catchClause().optUnwrap();
                    out.add(c.bindingName());
                    out.add(c.exceptionType());
                    out.add(c.body());
                }
                if (e.finallyBlock().optIsSome()) out.add(e.finallyBlock().optUnwrap());
            }
            case SourceExpr.Tuple e -> out.addAll(e.exprs());
        }
        return out;
    }

    public static Opt<TList<Entry>> scopeAt(TList<Metas.ScopeEvent> events, SourceId source, long statement) {
        var frames = new ArrayList<TList<Entry>>();
        frames.add(new TList<>());
        for (var ev : events) {
            var top = frames.get(frames.size() - 1);
            switch (ev.kind()) {
                case BEGIN -> frames.add(new TList<>());
                case END -> {
                    if (frames.size() > 1) frames.remove(frames.size() - 1);
                }
                case PUSH -> top.add(new Entry(ev.name(), ev.meta()));
                case SET -> {
                    for (int i = top.size() - 1; i >= 0; i--) {
                        if (top.get(i).name().equals(ev.name())) {
                            top.set(i, new Entry(ev.name(), ev.meta()));
                            break;
                        }
                    }
                }
                case POP -> {
                    for (int i = top.size() - 1; i >= 0; i--) {
                        if (top.get(i).name().equals(ev.name())) {
                            top.removeAt(i);
                            break;
                        }
                    }
                }
                case ENTER -> {
                    if (ev.loc().source().equals(source)
                            && ev.loc().expr().optIsSome()
                            && ev.loc().expr().optUnwrap() == statement) {
                        return Opt.some(TList.from(top));
                    }
                }
            }
        }
        return Opt.none();
    }
}
