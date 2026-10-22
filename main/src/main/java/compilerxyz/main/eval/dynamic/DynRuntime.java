package compilerxyz.main.eval.dynamic;

import static compilerxyz.prelude.util.MustCast.mustCast;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.CompileId;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.EvalContextInternal;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.Expr;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.expr.StagedExprCell;
import compilerxyz.main.eval.value.ConstHost;
import compilerxyz.main.eval.value.list.ListStore;
import compilerxyz.main.eval.value.map.TypeMapDynamic;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.GenAspectDynamicBoxed;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenLists;
import compilerxyz.main.gen.GenOptions;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.CodegenDescriptor;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import java.lang.reflect.Array;
import java.util.Iterator;

public final class DynRuntime {
    private DynRuntime() {}

    @CodegenDescriptor
    public static Type listType(ExprContext xx, Type elementType) {
        return GenLists.listDynamic(xx, elementType);
    }

    @CodegenDescriptor
    public static Type optionType(ExprContext xx, Type elementType) {
        return GenOptions.optionDynamic(xx, elementType);
    }

    @CodegenDescriptor
    public static Type mapType(ExprContext xx, Type keyType, Type valueType) {
        for (var check : new Type[] {keyType, valueType}) {
            if (GenAspectDynamicBoxed.getMaybe(xx, check).optIsSome()) {
                throw new DynPanic("map type argument cannot be the unboxed primitive " + check.typeDesc()
                        + "; use its boxed type (e.g. types.int_box)");
            }
        }
        return new TypeMapDynamic<>(keyType, valueType);
    }

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
                        liftCell.currentExceptionType(),
                        liftCell.loc().site());
                var liftRes = GenAspectLift.get(liftXx, liftCell.loc(), liftCell.sourceType());
                if (liftRes.hasErrors()) {
                    throw new IllegalStateException("__stage: AspectLift lookup failed for source type "
                            + liftCell.sourceType().typeDesc()
                            + ": "
                            + liftRes.errorsOpt().optUnwrapOr(new TList<>()));
                }
                var liftAspect = liftRes.requireValue("DynRuntime::__stage AspectLift.get");
                var liftedRes = liftAspect.aspectLiftLift(liftXx, liftCell.loc(), valArg);
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

    @CodegenDescriptor
    public static Object constHost(StagedExprCell cell) {
        return copyHost(MustCast.mustCast(ValueObjectConst.class, cell.value()).data());
    }

    private static Object copyHost(Object host) {
        return switch (host) {
            case TList<?> list -> {
                var out = new TList<Object>(list.size());
                for (var element : list) out.add(copyHost(element));
                yield out;
            }
            case TOrderedMap<?, ?> map -> {
                var keys = new TList<Object>(map.size());
                var values = new TList<Object>(map.size());
                for (var entry : map.entrySet()) {
                    keys.add(entry.getKey());
                    values.add(copyHost(entry.getValue()));
                }
                yield ConstHost.hostMap(keys, values);
            }
            case ListStore store -> store.listStoreCopy();
            case Opt.Some<?> some -> Opt.some(copyHost(some.value()));
            case EResult<?> result ->
                result.hasErrors() ? result : EResult.ok(copyHost(result.requireValue("DynRuntime::constHost ok")));
            default -> {
                if (!host.getClass().isArray()) yield host;
                var n = Array.getLength(host);
                var component = host.getClass().getComponentType();
                var out = Array.newInstance(component, n);
                if (component.isPrimitive()) {
                    System.arraycopy(host, 0, out, 0, n);
                } else {
                    for (int i = 0; i < n; i++) Array.set(out, i, copyHost(Array.get(host, i)));
                }
                yield out;
            }
        };
    }

    @CodegenDescriptor
    public static <K, V> V mapGet(TOrderedMap<K, V> map, K key) {
        var found = map.getOpt(key);
        if (found.optIsNone()) {
            throw new DynPanic("map.get: missing key " + key);
        }
        return found.optUnwrap();
    }

    @CodegenDescriptor
    public static <K, V> boolean mapHas(TOrderedMap<K, V> map, K key) {
        return map.containsKey(key);
    }

    public static Unique freshUnique(EvalContext vx) {
        var ix = EvalContextInternal.ix(vx);
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
                        case StageLowerValue.Val v -> new Expr(loc, new Expr.Literal(new Expr.LitValue(v.value())));
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
            case Expr.Pipe p ->
                new Expr(
                        loc,
                        new Expr.Pipe(
                                cloneWithLowerSubstitutions(p.self(), sd, ld, values, grafts),
                                cloneWithLowerSubstitutions(p.func(), sd, ld, values, grafts),
                                cloneWithLowerSubstitutions(p.arg(), sd, ld, values, grafts)));
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
            case Expr.Loop lp ->
                new Expr(loc, new Expr.Loop(cloneWithLowerSubstitutions(lp.body(), sd, ld, values, grafts)));
            case Expr.Foreach fe ->
                new Expr(
                        loc,
                        new Expr.Foreach(
                                cloneWithLowerSubstitutions(fe.base(), sd, ld, values, grafts),
                                cloneWithLowerSubstitutions(fe.elementKey(), sd, ld, values, grafts),
                                cloneWithLowerSubstitutions(fe.body(), sd, ld, values, grafts)));
            case Expr.Match m -> {
                var newBase = cloneWithLowerSubstitutions(m.base(), sd, ld, values, grafts);
                var newElementKey = cloneWithLowerSubstitutions(m.elementKey(), sd, ld, values, grafts);
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
                yield new Expr(loc, new Expr.Match(newBase, newCases, newElementKey, newFallback));
            }
            case Expr.Try t -> {
                var newBody = cloneWithLowerSubstitutions(t.body(), sd, ld, values, grafts);
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
                yield new Expr(loc, new Expr.Try(newBody, newCatch, newFinally));
            }
        };
    }
}
