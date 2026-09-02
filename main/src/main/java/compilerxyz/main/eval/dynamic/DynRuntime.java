package compilerxyz.main.eval.dynamic;

import static compilerxyz.prelude.util.MustCast.mustCast;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.CompileId;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.Expr;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.expr.StagedExprCell;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.CodegenDescriptor;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.Iterator;

public final class DynRuntime {
    private DynRuntime() {}

    @CodegenDescriptor
    public static StagedExprCell stage(StagedExprCell treeCell, int lowerCount, Object[] rest) {
        var tree = mustCast(Expr.class, treeCell.value());
        var values = new TList<StageLowerValue>(lowerCount);
        var i = 0;
        for (var k = 0; k < lowerCount; k++) {
            var liftArg = rest[i];
            var valArg = rest[i + 1];
            i += 2;
            if (liftArg == null) {
                var cell = (StagedExprCell) valArg;
                var cellValue = cell.value();
                if (cellValue instanceof Expr expr) {
                    values.add(new StageLowerValue.ExprStage(expr));
                } else {
                    values.add(new StageLowerValue.Val((Value) cellValue));
                }
            } else {
                var liftCell = (StageLiftCell) liftArg;
                var liftXx = new ExprContext(
                        liftCell.scope(),
                        liftCell.target(),
                        liftCell.vx(),
                        liftCell.importState(),
                        liftCell.currentExceptionType());
                var liftRes = GenAspectLift.get(liftXx, liftCell.loc(), liftCell.sourceType());
                if (liftRes.hasErrors()) {
                    throw new IllegalStateException("__stage: AspectLift lookup failed for source type "
                            + liftCell.sourceType().typeDesc()
                            + ": "
                            + liftRes.errorsOpt().optUnwrapOr(new TList<>()));
                }
                var liftAspect = liftRes.requireValue("DynRuntime::__stage AspectLift.get");
                var liftedRes = liftAspect.aspectLiftLift(liftXx, liftCell.loc(), Opt.ofNullable(valArg));
                if (liftedRes.hasErrors()) {
                    throw new IllegalStateException("__stage: AspectLift.lift failed for source type "
                            + liftCell.sourceType().typeDesc()
                            + ": "
                            + liftedRes.errorsOpt().optUnwrapOr(new TList<>()));
                }
                var lifted = liftedRes.requireValue("DynRuntime::__stage AspectLift.lift");
                values.add(new StageLowerValue.Val(lifted));
            }
        }
        var grafts = new TList<CodeValue>(rest.length - i);
        while (i < rest.length) {
            var raw = rest[i];
            CodeValue graft;
            if (raw instanceof CodeValue cv) {
                graft = cv;
            } else if (raw instanceof Code code) {
                graft = new CodeValue(code, EvalOutcome.NO_VALUE);
            } else {
                throw new RuntimeException("__stage: graft argument is neither CodeValue nor Code: "
                        + (raw == null ? "null" : raw.getClass().getName()));
            }
            grafts.add(graft);
            i += 1;
        }
        var valueIter = values.iterator();
        var graftIter = grafts.iterator();
        var newTree = cloneWithLowerSubstitutions(tree, 0, 0, valueIter, graftIter);
        return new StagedExprCell(newTree);
    }

    public static Unique freshUnique(EvalContext vx) {
        var ix = vx.ix;
        var compileId = new CompileId(ix.importState, ix.sourceId);
        return new Unique.Source(compileId, ix.uniqueCounter.next());
    }

    private sealed interface StageLowerValue {
        record ExprStage(Expr expr) implements StageLowerValue {}

        record Val(Value value) implements StageLowerValue {}
    }

    private static Expr cloneWithLowerSubstitutions(
            Expr expr, int sd, int ld, Iterator<StageLowerValue> values, Iterator<CodeValue> grafts) {
        var loc = expr.loc();
        return switch (expr.variant()) {
            case Expr.Lower lower -> {
                var newLd = ld + 1;
                if (newLd - sd == 1) {
                    if (!values.hasNext()) {
                        throw new IllegalStateException("__stage: not enough lower values for substitution");
                    }
                    yield switch (values.next()) {
                        case StageLowerValue.ExprStage e -> e.expr();
                        case StageLowerValue.Val v ->
                            new Expr(loc, new Expr.Literal(new Expr.LitValue(v.value(), true)));
                    };
                }
                yield new Expr(
                        loc, new Expr.Lower(cloneWithLowerSubstitutions(lower.expr(), sd, newLd, values, grafts)));
            }
            case Expr.Stage stage ->
                new Expr(loc, new Expr.Stage(cloneWithLowerSubstitutions(stage.expr(), sd + 1, ld, values, grafts)));
            case Expr.Graft graft -> {
                Expr.Graft next =
                        switch (graft) {
                            case Expr.Graft.Pending p -> {
                                if (sd == 0) {
                                    if (!grafts.hasNext()) {
                                        throw new IllegalStateException(
                                                "__stage: not enough graft values for substitution");
                                    }
                                    yield new Expr.Graft.Evaluated(grafts.next());
                                }
                                yield new Expr.Graft.Pending(
                                        cloneWithLowerSubstitutions(p.expr(), sd, ld, values, grafts));
                            }
                            case Expr.Graft.Evaluated e -> e;
                        };
                yield new Expr(loc, next);
            }
            case Expr.Literal lit -> expr;
            case Expr.ScopeUse u -> expr;
            case Expr.Labeled lb ->
                new Expr(
                        loc,
                        new Expr.Labeled(lb.label(), cloneWithLowerSubstitutions(lb.inner(), sd, ld, values, grafts)));
            case Expr.Return rt -> {
                Opt<Expr> newValue;
                switch (rt.value()) {
                    case Opt.Some<Expr> someV ->
                        newValue = Opt.some(cloneWithLowerSubstitutions(someV.value(), sd, ld, values, grafts));
                    case Opt.None<Expr> none -> newValue = Opt.none();
                }
                yield new Expr(loc, new Expr.Return(rt.label(), newValue));
            }
            case Expr.As a ->
                new Expr(
                        loc,
                        new Expr.As(
                                cloneWithLowerSubstitutions(a.base(), sd, ld, values, grafts),
                                cloneWithLowerSubstitutions(a.target(), sd, ld, values, grafts)));
            case Expr.Call c ->
                new Expr(
                        loc,
                        new Expr.Call(
                                cloneWithLowerSubstitutions(c.func(), sd, ld, values, grafts),
                                cloneWithLowerSubstitutions(c.arg(), sd, ld, values, grafts)));
            case Expr.Bind b ->
                new Expr(
                        loc,
                        new Expr.Bind(
                                cloneWithLowerSubstitutions(b.base(), sd, ld, values, grafts),
                                cloneWithLowerSubstitutions(b.value(), sd, ld, values, grafts)));
            case Expr.Set s ->
                new Expr(
                        loc,
                        new Expr.Set(
                                cloneWithLowerSubstitutions(s.base(), sd, ld, values, grafts),
                                cloneWithLowerSubstitutions(s.value(), sd, ld, values, grafts)));
            case Expr.Field f ->
                new Expr(
                        loc,
                        new Expr.Field(
                                cloneWithLowerSubstitutions(f.base(), sd, ld, values, grafts),
                                cloneWithLowerSubstitutions(f.key(), sd, ld, values, grafts)));
            case Expr.Seq seq -> {
                var newExprs = new TList<Expr>();
                for (var sub : seq.exprs()) {
                    newExprs.add(cloneWithLowerSubstitutions(sub, sd, ld, values, grafts));
                }
                yield new Expr(loc, new Expr.Seq(newExprs));
            }
            case Expr.Tuple tup -> {
                var newElems = new TList<Expr>();
                for (var sub : tup.elements()) {
                    newElems.add(cloneWithLowerSubstitutions(sub, sd, ld, values, grafts));
                }
                yield new Expr(loc, new Expr.Tuple(newElems));
            }
            case Expr.Record rec -> {
                var newEntries = new TList<Expr.RecordEntry>();
                for (var entry : rec.entries()) {
                    newEntries.add(new Expr.RecordEntry(
                            cloneWithLowerSubstitutions(entry.key(), sd, ld, values, grafts),
                            cloneWithLowerSubstitutions(entry.value(), sd, ld, values, grafts)));
                }
                yield new Expr(loc, new Expr.Record(newEntries));
            }
            case Expr.If ifExpr -> {
                var newConds = new TList<Expr.ConditionBranch>();
                for (var branch : ifExpr.conditions()) {
                    newConds.add(new Expr.ConditionBranch(
                            cloneWithLowerSubstitutions(branch.condition(), sd, ld, values, grafts),
                            cloneWithLowerSubstitutions(branch.body(), sd, ld, values, grafts)));
                }
                Opt<Expr> newElse;
                switch (ifExpr.else_()) {
                    case Opt.Some<Expr> some ->
                        newElse = Opt.some(cloneWithLowerSubstitutions(some.value(), sd, ld, values, grafts));
                    case Opt.None<Expr> none -> newElse = Opt.none();
                }
                yield new Expr(loc, new Expr.If(newConds, newElse));
            }
            case Expr.OperatorBinary op ->
                new Expr(
                        loc,
                        new Expr.OperatorBinary(
                                op.op(),
                                cloneWithLowerSubstitutions(op.base(), sd, ld, values, grafts),
                                cloneWithLowerSubstitutions(op.reference(), sd, ld, values, grafts)));
            case Expr.OperatorUnary op ->
                new Expr(
                        loc,
                        new Expr.OperatorUnary(
                                op.op(), cloneWithLowerSubstitutions(op.base(), sd, ld, values, grafts)));
            case Expr.Foreach fe ->
                new Expr(
                        loc,
                        new Expr.Foreach(
                                cloneWithLowerSubstitutions(fe.base(), sd, ld, values, grafts),
                                cloneWithLowerSubstitutions(fe.elementKey(), sd, ld, values, grafts),
                                cloneWithLowerSubstitutions(fe.body(), sd, ld, values, grafts)));
            case Expr.Match m -> {
                var newCases = new TList<Expr.MatchBranch>();
                for (var c : m.cases()) {
                    newCases.add(new Expr.MatchBranch(
                            cloneWithLowerSubstitutions(c.condition(), sd, ld, values, grafts),
                            cloneWithLowerSubstitutions(c.body(), sd, ld, values, grafts)));
                }
                Opt<Expr> newFallback;
                switch (m.fallback()) {
                    case Opt.Some<Expr> some ->
                        newFallback = Opt.some(cloneWithLowerSubstitutions(some.value(), sd, ld, values, grafts));
                    case Opt.None<Expr> none -> newFallback = Opt.none();
                }
                yield new Expr(
                        loc,
                        new Expr.Match(
                                cloneWithLowerSubstitutions(m.base(), sd, ld, values, grafts),
                                newCases,
                                cloneWithLowerSubstitutions(m.elementKey(), sd, ld, values, grafts),
                                newFallback));
            }
            case Expr.Try t -> {
                Opt<Expr.TryCatch> newCatch;
                switch (t.catchClause()) {
                    case Opt.Some<Expr.TryCatch> someCatch -> {
                        var c = someCatch.value();
                        newCatch = Opt.some(new Expr.TryCatch(
                                cloneWithLowerSubstitutions(c.bindingName(), sd, ld, values, grafts),
                                cloneWithLowerSubstitutions(c.exceptionType(), sd, ld, values, grafts),
                                cloneWithLowerSubstitutions(c.body(), sd, ld, values, grafts)));
                    }
                    case Opt.None<Expr.TryCatch> none -> newCatch = Opt.none();
                }
                Opt<Expr> newFinally;
                switch (t.finallyBlock()) {
                    case Opt.Some<Expr> someFin ->
                        newFinally = Opt.some(cloneWithLowerSubstitutions(someFin.value(), sd, ld, values, grafts));
                    case Opt.None<Expr> none -> newFinally = Opt.none();
                }
                yield new Expr(
                        loc,
                        new Expr.Try(
                                cloneWithLowerSubstitutions(t.body(), sd, ld, values, grafts), newCatch, newFinally));
            }
        };
    }
}
