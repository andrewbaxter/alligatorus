package compilerxyz.main.eval;

import compilerxyz.main.eval.value.record.Record;
import compilerxyz.main.eval.value.record.RecordField;
import compilerxyz.main.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.eval.value.tuple.TupleElement;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBoolConst;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectIterable;
import compilerxyz.main.gen.GenAspectOperatorAdd;
import compilerxyz.main.gen.GenAspectOperatorAnd;
import compilerxyz.main.gen.GenAspectOperatorBitNot;
import compilerxyz.main.gen.GenAspectOperatorDiv;
import compilerxyz.main.gen.GenAspectOperatorEq;
import compilerxyz.main.gen.GenAspectOperatorGe;
import compilerxyz.main.gen.GenAspectOperatorGt;
import compilerxyz.main.gen.GenAspectOperatorLe;
import compilerxyz.main.gen.GenAspectOperatorLt;
import compilerxyz.main.gen.GenAspectOperatorMod;
import compilerxyz.main.gen.GenAspectOperatorMul;
import compilerxyz.main.gen.GenAspectOperatorNe;
import compilerxyz.main.gen.GenAspectOperatorNeg;
import compilerxyz.main.gen.GenAspectOperatorOr;
import compilerxyz.main.gen.GenAspectOperatorShl;
import compilerxyz.main.gen.GenAspectOperatorShr;
import compilerxyz.main.gen.GenAspectOperatorSub;
import compilerxyz.main.gen.GenAspectOperatorUshr;
import compilerxyz.main.gen.GenAspectOperatorXor;
import compilerxyz.main.gen.GenAspectSet;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenAspectUnion;
import compilerxyz.main.gen.IfBranch;
import compilerxyz.main.gen.TargetCase;
import compilerxyz.main.gen.TargetMatchFallback;
import compilerxyz.main.gen.TryCatchClause;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.IOrderedMap;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;

public final class ConstEval {
    private ConstEval() {}

    public static EResult<EvalRes> constEval(ExprContext xx, Expr expr) {
        var log = xx.vx.ix.metaLog;
        log.enter(xx, expr.loc());
        try {
            var res = constEvalInner(xx, expr);
            var evOpt = res.valueOpt();
            if (evOpt.optIsSome()) {
                var value = evOpt.optUnwrap().cv().value();
                if (value.optIsSome()) log.recordExpr(xx, expr.loc(), value.optUnwrap());
            }
            return res;
        } finally {
            log.exit();
        }
    }

    private static EResult<EvalRes> constEvalInner(ExprContext xx, Expr expr) {
        var loc = expr.loc();
        return switch (expr.variant()) {
            case Expr.Literal lit -> {
                var litVal = lit.value();
                Value v;
                if (!litVal.once) {
                    v = litVal.value.optUnwrap();
                } else {
                    if (litVal.value.optIsNone()) throw new IllegalStateException("Once Literal already consumed");
                    v = litVal.value.optUnwrap();
                    litVal.value = Opt.none();
                }
                yield EResult.ok(EvalRes.of(new CodeValue(Code.none(loc), EvalOutcome.ofValue(v)), xx.scope));
            }
            case Expr.ScopeUse p -> {
                var erb = new Erb(loc);
                var keyOpt = erb.mustCv(xx, constEvalOperand(xx, p.key()));
                if (keyOpt.optIsNone()) yield EResult.errs(erb.errors());
                var key = keyOpt.optUnwrap();
                var keyVal = (Opt.Some<Value>) key.value();
                var keyConstRes = Eval.extractConstKey(xx, keyVal.value());
                var keyConstOpt = erb.mustRes(keyConstRes);
                if (keyConstOpt.optIsNone()) yield EResult.errs(erb.errors());
                var ck = keyConstOpt.optUnwrap();
                ScopeLevel.Entry found = null;
                lookupEntryLoop:
                for (int i = xx.scope.levels.size() - 1; i >= 0; i--) {
                    switch (xx.scope.levels.get(i)) {
                        case ScopeLevel.Entry entry -> {
                            if (entry.name.optIsSome() && entry.name.optUnwrap().equals(ck)) {
                                found = entry;
                                break lookupEntryLoop;
                            }
                        }
                        case ScopeLevel.Temp t -> {}
                        case ScopeLevel.Labeled l -> {}
                        case ScopeLevel.Catch c -> {}
                        case ScopeLevel.Finally f -> {}
                    }
                }
                if (found == null) {
                    erb.errPush("No bound value with id [" + ck + "]");
                    yield EResult.errs(erb.errors());
                }
                var setter = new ScopeEntrySetter(xx.scope, found);
                yield EResult.ok(EvalRes.of(found.binding.bindingUse(xx, loc, setter, key.code()), xx.scope));
            }
            case Expr.Call p -> {
                var erb = new Erb(loc);
                int operandBase = xx.scope.levelCount();
                var fopt = constEvalOperandPending(xx, erb, p.func());
                var aopt = erb.mustCv(xx, constEvalOperand(xx, p.arg()));
                var pendingBlocks = operandPendingClose(xx, operandBase);
                if (fopt.optIsNone() || aopt.optIsNone()) yield EResult.errs(erb.errors());
                var func = fopt.optUnwrap();
                var arg = aopt.optUnwrap();
                var fv = (Opt.Some<Value>) func.value();
                var funcType = fv.value().valueType();
                var aspectFnOpt = erb.mustEr(GenAspectFunction.get(xx, loc, funcType));
                if (aspectFnOpt.optIsNone()) yield EResult.errs(erb.errors());
                var aspectFn = aspectFnOpt.optUnwrap();
                var calleeExc = aspectFn.aspectFunctionExceptionType(xx, loc, func);
                var callOpt = erb.mustErMaybeUnreachable(aspectFn.aspectFunctionCall(xx, loc, func, arg));
                if (callOpt.optIsNone()) yield EResult.errs(erb.errors());
                Opt<CodeValue> wrapped;
                {
                    var callCv = callOpt.optUnwrap();
                    if (calleeExc.optIsNone()) {
                        wrapped = Opt.some(callCv);
                    } else {
                        var excType = calleeExc.optUnwrap();
                        if (xx.currentExceptionType.optIsNone()) {
                            erb.errPush(
                                    "Cannot call a function that may throw from a function that did not declare an exception type");
                            wrapped = Opt.none();
                        } else {
                            var exc = xx.currentExceptionType.optUnwrap();
                            var destRes = ScopeChains.excDest(xx, loc);
                            if (destRes.hasErrors()) {
                                destRes.drainInto(erb.errors());
                                wrapped = Opt.none();
                            } else {
                                var chainDest = destRes.requireValue("maybeWrapCallException chain dest");
                                var siteKey = new JumpKey();
                                var blockRes = erb.mustEr(
                                        xx.target.targetCastException(xx, loc, siteKey, excType, exc, chainDest));
                                if (blockRes.optIsNone()) {
                                    wrapped = Opt.none();
                                } else if (blockRes.optUnwrap().optIsSome()) {
                                    var skip = new JumpKey();
                                    var opt = erb.mustEr(xx.target.targetJump(loc, skip));
                                    var landOpt =
                                            erb.mustErMaybeUnreachable(xx.target.targetLand(xx, loc, skip, Opt.none()));
                                    if (opt.optIsNone() || landOpt.optIsNone()) {
                                        wrapped = Opt.none();
                                    } else {
                                        var wrappedOpt =
                                                erb.mustEr(xx.target.targetGuarded(loc, siteKey, callCv.code()));
                                        if (wrappedOpt.optIsNone()) {
                                            wrapped = Opt.none();
                                        } else {
                                            var fullOpt = seqCode(
                                                    xx,
                                                    loc,
                                                    erb,
                                                    TList.of(
                                                            wrappedOpt.optUnwrap(),
                                                            opt.optUnwrap(),
                                                            blockRes.optUnwrap().optUnwrap(),
                                                            landOpt.optUnwrap().code()));
                                            wrapped = fullOpt.optIsNone()
                                                    ? Opt.none()
                                                    : Opt.some(new CodeValue(fullOpt.optUnwrap(), callCv.outcome()));
                                        }
                                    }
                                } else {
                                    if (chainDest.optIsNone()) {
                                        wrapped = Opt.some(callCv);
                                    } else {
                                        var opt = erb.mustEr(
                                                xx.target.targetGuarded(loc, chainDest.optUnwrap(), callCv.code()));
                                        if (opt.optIsNone()) {
                                            wrapped = Opt.none();
                                        } else {
                                            wrapped = Opt.some(new CodeValue(opt.optUnwrap(), callCv.outcome()));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (wrapped.optIsNone()) yield EResult.errs(erb.errors());
                var callOut = withPendingBlocks(xx, loc, erb, wrapped.optUnwrap(), pendingBlocks);
                if (callOut.optIsNone()) yield EResult.errs(erb.errors());
                yield EResult.ok(EvalRes.of(callOut.optUnwrap(), xx.scope));
            }
            case Expr.Set e -> {
                var erb = new Erb(loc);
                var bopt = erb.mustCv(xx, constEvalOperand(xx, e.base()));
                var vopt = erb.mustCv(xx, constEvalOperand(xx, e.value()));
                if (bopt.optIsNone() || vopt.optIsNone()) yield EResult.errs(erb.errors());
                var base = bopt.optUnwrap();
                var value = vopt.optUnwrap();
                var bv = (Opt.Some<Value>) base.value();
                var baseType = bv.value().valueType();
                var setOpt = erb.mustEr(GenAspectSet.get(xx, loc, baseType));
                if (setOpt.optIsNone()) yield EResult.errs(erb.errors());
                var out = erb.mustErMaybeUnreachable(setOpt.optUnwrap().aspectSetSet(xx, loc, base, value));
                if (out.optIsNone()) yield EResult.errs(erb.errors());
                var setCv = out.optUnwrap();
                if (setCv.value().optIsSome()) {
                    erb.errPush("set: assignment must not yield a value");
                    yield EResult.errs(erb.errors());
                }
                yield EResult.ok(EvalRes.of(setCv, xx.scope));
            }
            case Expr.Field e -> {
                var erb = new Erb(loc);
                var bopt = erb.mustCv(xx, constEvalOperand(xx, e.base()));
                var fopt = erb.mustCv(xx, constEvalOperand(xx, e.key()));
                if (bopt.optIsNone() || fopt.optIsNone()) yield EResult.errs(erb.errors());
                var base = bopt.optUnwrap();
                var field = fopt.optUnwrap();
                var bv = (Opt.Some<Value>) base.value();
                var baseType = bv.value().valueType();
                var aspectFieldOpt = erb.mustEr(GenAspectField.get(xx, loc, baseType));
                if (aspectFieldOpt.optIsNone()) yield EResult.errs(erb.errors());
                var aspectFieldField = aspectFieldOpt.optUnwrap();
                var primaryOpt = erb.mustEr(aspectFieldField.aspectFieldField(xx, loc, base, field));
                if (primaryOpt.optIsNone()) yield EResult.errs(erb.errors());
                yield EResult.ok(EvalRes.of(primaryOpt.optUnwrap(), xx.scope));
            }
            case Expr.Bind ignored -> EResult.err(loc, "Bind expression must be directly inside a block");
            case Expr.Seq e -> {
                var erb = new Erb(loc);
                int baseLevelCount = xx.scope.levelCount();
                var consumer = xx.scope.pushLabeled(Opt.<String>none());
                var codes = new TList<Code>();
                var entries = new TList<ScopeLevel.Entry>();
                Opt<Value> lastVal = Opt.none();
                CodeValue pendingDiscard = null;
                boolean controlEnded = false;
                for (var sub : e.exprs()) {
                    xx.vx.ix.metaLog.scopeEnter(sub.loc());
                    if (controlEnded) {
                        erb.errors()
                                .add(new ExprErr(
                                        sub.loc(),
                                        TList.of(new ExprSubErr.Opaque(
                                                "Unreachable: control flow ended at prior expression"))));
                        continue;
                    }
                    if (pendingDiscard != null) {
                        var dropOpt = erb.mustErMaybeUnreachable(Eval.dropValue(xx, sub.loc(), pendingDiscard));
                        if (dropOpt.optIsNone()) yield EResult.errs(erb.errors());
                        codes.add(dropOpt.optUnwrap());
                        pendingDiscard = null;
                    }
                    Opt<Expr.Bind> bindOpt =
                            switch (sub.variant()) {
                                case Expr.Bind b -> Opt.some(b);
                                case Expr.As a -> Opt.none();
                                case Expr.Call c -> Opt.none();
                                case Expr.Field f -> Opt.none();
                                case Expr.Foreach f -> Opt.none();
                                case Expr.Graft.Pending p -> Opt.none();
                                case Expr.Graft.Evaluated g -> Opt.none();
                                case Expr.If i -> Opt.none();
                                case Expr.Labeled l -> Opt.none();
                                case Expr.Literal l -> Opt.none();
                                case Expr.Lower l -> Opt.none();
                                case Expr.Match m -> Opt.none();
                                case Expr.OperatorBinary o -> Opt.none();
                                case Expr.OperatorUnary o -> Opt.none();
                                case Expr.Record r -> Opt.none();
                                case Expr.Return r -> Opt.none();
                                case Expr.ScopeUse s -> Opt.none();
                                case Expr.Seq s -> Opt.none();
                                case Expr.Set s -> Opt.none();
                                case Expr.Stage s -> Opt.none();
                                case Expr.Try t -> Opt.none();
                                case Expr.Tuple t -> Opt.none();
                            };
                    if (bindOpt.optIsSome()) {
                        var bind = bindOpt.optUnwrap();
                        var bopt = erb.mustCv(xx, constEvalOperand(xx, bind.base()));
                        var vres = constEval(xx, bind.value());
                        if (vres.valueOpt().optIsNone()) {
                            vres.drainInto(erb.errors());
                            yield EResult.errs(erb.errors());
                        }
                        vres.drainInto(erb.errors());
                        var vev = vres.requireValue("constEvalSeq bind value");
                        xx.scope.adopt(vev.scope());
                        if (bopt.optIsNone()) yield EResult.errs(erb.errors());
                        var base = bopt.optUnwrap();
                        var value = vev.cv();
                        if (base.value().optIsNone()) {
                            erb.errPush("Bind: base has no compile-time value");
                            yield EResult.errs(erb.errors());
                        }
                        var ckOpt = erb.mustRes(
                                Eval.extractConstKey(xx, base.value().optUnwrap()));
                        if (ckOpt.optIsNone()) yield EResult.errs(erb.errors());
                        codes.add(base.code());
                        if (value.value().optIsNone()) {
                            erb.errors()
                                    .add(new ExprErr(
                                            sub.loc(),
                                            TList.of(new ExprSubErr.Opaque(
                                                    value.controlEnded()
                                                            ? "Cannot bind result of expression that ends control flow"
                                                            : "Cannot bind an expression that yields no value"))));
                            yield EResult.errs(erb.errors());
                        }
                        var valType = value.value().optUnwrap().valueType();
                        var bindAspectOpt = erb.mustEr(GenAspectBind.get(xx, sub.loc(), valType));
                        if (bindAspectOpt.optIsNone()) yield EResult.errs(erb.errors());
                        var brOpt = erb.mustEr(bindAspectOpt.optUnwrap().aspectBindBind(xx, sub.loc(), value));
                        if (brOpt.optIsNone()) yield EResult.errs(erb.errors());
                        var br = brOpt.optUnwrap();
                        codes.add(br.code());
                        entries.add(xx.scope.pushNamed(ckOpt.optUnwrap(), br.binding()));
                    } else {
                        var rres = constEval(xx, sub);
                        if (rres.valueOpt().optIsNone()) {
                            rres.drainInto(erb.errors());
                            yield EResult.errs(erb.errors());
                        }
                        rres.drainInto(erb.errors());
                        var rev = rres.requireValue("constEvalSeq entry");
                        xx.scope.adopt(rev.scope());
                        var r = rev.cv();
                        codes.add(r.code());
                        switch (r.outcome()) {
                            case EvalOutcome.ControlEnded ce -> {
                                controlEnded = true;
                                lastVal = Opt.none();
                                continue;
                            }
                            case EvalOutcome.NoValue nv -> {}
                            case EvalOutcome.HasValue hv -> {}
                        }
                        pendingDiscard = new CodeValue(Code.none(sub.loc()), r.outcome());
                    }
                }
                if (pendingDiscard != null) {
                    var dropOpt = erb.mustErMaybeUnreachable(Eval.dropValue(xx, loc, pendingDiscard));
                    if (dropOpt.optIsNone()) yield EResult.errs(erb.errors());
                    codes.add(dropOpt.optUnwrap());
                    pendingDiscard = null;
                }
                for (int i = entries.size() - 1; i >= 0; i--) {
                    var top = xx.scope.levels.get(xx.scope.levels.size() - 1);
                    ScopeLevel.Entry current;
                    switch (top) {
                        case ScopeLevel.Entry en -> current = en;
                        case ScopeLevel.Labeled lb ->
                            throw new AssertionError("seq unwind: top level is not the expected entry");
                        case ScopeLevel.Temp t ->
                            throw new AssertionError("seq unwind: top level is not the expected entry");
                        case ScopeLevel.Catch c ->
                            throw new AssertionError("seq unwind: top level is not the expected entry");
                        case ScopeLevel.Finally f ->
                            throw new AssertionError("seq unwind: top level is not the expected entry");
                    }
                    if (current.scopeLevelEmission().id != entries.get(i).scopeLevelEmission().id) {
                        throw new AssertionError("seq unwind: top level is not the expected entry");
                    }
                    var dropRes = current.binding.bindingDrop(xx, loc);
                    if (dropRes.hasErrors()) {
                        dropRes.drainInto(erb.errors());
                        yield EResult.errs(erb.errors());
                    }
                    var code = dropRes.requireValue("entryClose drop");
                    var spliceOpt = blocksSplice(xx, loc, current.scopeLevelEmission().blocks, erb);
                    if (spliceOpt.optIsNone()) yield EResult.errs(erb.errors());
                    var splice = spliceOpt.optUnwrap();
                    codes.add(code);
                    codes.add(splice);
                    xx.scope.popLevel();
                }
                var seqOpt = erb.mustErMaybeUnreachable(xx.target.targetSeq(loc, codes));
                if (seqOpt.optIsNone()) yield EResult.errs(erb.errors());
                var innerCv = new CodeValue(
                        seqOpt.optUnwrap(),
                        lastVal.optIsSome()
                                ? EvalOutcome.ofValue(lastVal.optUnwrap())
                                : (controlEnded ? EvalOutcome.CONTROL_ENDED : EvalOutcome.NO_VALUE));
                var innerRes = EvalRes.of(innerCv, xx.scope);
                yield labeledConsume(
                        xx,
                        loc,
                        consumer,
                        baseLevelCount,
                        erb.errors().isEmpty() ? EResult.ok(innerRes) : EResult.mixed(innerRes, erb.errors()));
            }
            case Expr.Stage e -> {
                var lowers = new TList<Expr>();
                findMatchedLowersInner(e.expr(), 0, 0, lowers);
                var grafts = new TList<Expr>();
                findMatchedGraftsInner(e.expr(), 0, grafts);
                if (lowers.isEmpty() && grafts.isEmpty()) {
                    yield EResult.ok(
                            EvalRes.of(new CodeValue(Code.none(loc), EvalOutcome.ofValue(e.expr())), xx.scope));
                }
                var stageErb = new Erb(loc);
                var values = new TList<CodeValue>(lowers.size());
                for (var inner : lowers) {
                    var cvOpt = stageErb.mustCv(xx, constEvalOperand(xx, inner));
                    if (cvOpt.optIsNone()) yield EResult.errs(stageErb.errors());
                    values.add(cvOpt.optUnwrap());
                }
                var graftValues = new TList<CodeValue>(grafts.size());
                for (var inner : grafts) {
                    var opt = stageErb.mustCv(xx, constEvalOperand(xx, inner));
                    if (opt.optIsNone()) yield EResult.errs(stageErb.errors());
                    graftValues.add(opt.optUnwrap());
                }
                var staged = stageErb.mustEr(xx.target.targetStage(xx, loc, e.expr(), values, graftValues));
                if (staged.optIsNone()) yield EResult.errs(stageErb.errors());
                yield EResult.ok(EvalRes.of(staged.optUnwrap(), xx.scope));
            }
            case Expr.Lower lo -> constEval(xx, lo.expr());
            case Expr.Graft.Evaluated ev -> EResult.ok(EvalRes.of(ev.value(), xx.scope));
            case Expr.Graft.Pending ignored ->
                EResult.err(loc, "Graft outside of stage processing — its inner was never evaluated");
            case Expr.Tuple t -> {
                var erb = new Erb(loc);
                var elements = new TList<TupleElement>(t.elements().size());
                int operandBase = xx.scope.levelCount();
                for (int ei = 0; ei < t.elements().size(); ei++) {
                    var elExpr = t.elements().get(ei);
                    var elRes = ei == t.elements().size() - 1
                            ? elementResult(xx, constEvalOperand(xx, elExpr))
                            : elementResultPending(xx, constEvalOperand(xx, elExpr));
                    if (elRes.valueOpt().optIsNone()) {
                        elRes.drainInto(erb.errors());
                        continue;
                    }
                    if (elRes.valueOpt().optUnwrap().value().optIsNone()) {
                        erb.errors()
                                .add(new ExprErr(
                                        elExpr.loc(),
                                        TList.of(new ExprSubErr.Opaque(
                                                "Tuple element does not fall through (contains an unreachable"
                                                        + " control jump like throw/panic); not valid in element"
                                                        + " position"))));
                        continue;
                    }
                    elements.add(new TupleElement.EvalRes(elRes));
                }
                var pendingBlocks = operandPendingClose(xx, operandBase);
                if (!erb.errors().isEmpty()) yield EResult.errs(erb.errors());
                var tupleCodeOpt = blocksSplice(xx, loc, pendingBlocks, erb);
                if (tupleCodeOpt.optIsNone()) yield EResult.errs(erb.errors());
                var tupleCode = tupleCodeOpt.optUnwrap();
                yield EResult.ok(EvalRes.of(
                        new CodeValue(tupleCode, EvalOutcome.ofValue(new Tuple(IList.from(elements)))), xx.scope));
            }
            case Expr.Record r -> {
                var entries = new TOrderedMap<ConstKey, EResult<CodeValue>>();
                var keyCodes = new TList<Code>();
                var errors = new TList<ExprErr>();
                int operandBase = xx.scope.levelCount();
                for (int ei = 0; ei < r.entries().size(); ei++) {
                    var entry = r.entries().get(ei);
                    var kRes = elementResult(xx, constEvalOperand(xx, entry.key()));
                    if (kRes.hasErrors()) {
                        kRes.drainInto(errors);
                        continue;
                    }
                    var kCv = kRes.requireValue("ConstEval::record key");
                    if (!kCv.code().codeIsNone()) keyCodes.add(kCv.code());
                    if (kCv.value().optIsNone()) {
                        errors.add(new ExprErr(
                                loc, TList.of(new ExprSubErr.Opaque("Record: key has no compile-time value"))));
                        continue;
                    }
                    var keyRes = Eval.extractConstKey(xx, kCv.value().optUnwrap());
                    switch (keyRes) {
                        case Result.Ok<ConstKey, String> ok -> {
                            var valRes = ei == r.entries().size() - 1
                                    ? elementResult(xx, constEvalOperand(xx, entry.value()))
                                    : elementResultPending(xx, constEvalOperand(xx, entry.value()));
                            if (valRes.valueOpt().optIsNone()) {
                                valRes.drainInto(errors);
                            } else if (valRes.valueOpt().optUnwrap().value().optIsNone()) {
                                errors.add(new ExprErr(
                                        entry.value().loc(),
                                        TList.of(new ExprSubErr.Opaque(
                                                "Record field '" + ok.value().constKeyToCanonical()
                                                        + "' does not fall through (contains an unreachable control"
                                                        + " jump like throw/panic); not valid in field position"))));
                            } else {
                                entries.put(ok.value(), valRes);
                            }
                        }
                        case Result.Err<ConstKey, String> err ->
                            errors.add(new ExprErr(loc, TList.of(new ExprSubErr.Opaque(err.error()))));
                    }
                }
                var pendingBlocks = operandPendingClose(xx, operandBase);
                if (!errors.isEmpty()) yield EResult.errs(errors);
                if (!pendingBlocks.isEmpty()) {
                    var spliceErb = new Erb(loc);
                    var spliceOpt = blocksSplice(xx, loc, pendingBlocks, spliceErb);
                    if (spliceOpt.optIsNone()) yield EResult.errs(spliceErb.errors());
                    var splice = spliceOpt.optUnwrap();
                    keyCodes.add(splice);
                }
                var codeRes = xx.target.targetSeq(loc, keyCodes);
                if (codeRes.hasErrors()) yield codeRes.reerr();
                var rCode = codeRes.requireValue("record key codes");
                var fields = new TOrderedMap<ConstKey, RecordField>();
                for (var fe : entries.entrySet()) fields.put(fe.getKey(), new RecordField.EvalRes(fe.getValue()));
                yield EResult.ok(EvalRes.of(
                        new CodeValue(rCode, EvalOutcome.ofValue(new Record(IOrderedMap.from(fields)))), xx.scope));
            }
            case Expr.OperatorBinary e -> {
                var erb = new Erb(loc);
                int operandBase = xx.scope.levelCount();
                var bopt = constEvalOperandPending(xx, erb, e.base());
                var ropt = erb.mustCv(xx, constEvalOperand(xx, e.reference()));
                var pendingBlocks = operandPendingClose(xx, operandBase);
                if (bopt.optIsNone() || ropt.optIsNone()) yield EResult.errs(erb.errors());
                var base = bopt.optUnwrap();
                var bv = (Opt.Some<Value>) base.value();
                var baseType = bv.value().valueType();
                var reference = ropt.optUnwrap();
                var out =
                        switch (e.op()) {
                            case ADD -> {
                                var aopt = erb.mustEr(GenAspectOperatorAdd.get(xx, loc, baseType));
                                if (aopt.optIsNone()) yield Opt.<CodeValue>none();
                                yield erb.mustEr(aopt.optUnwrap().aspectOperatorAddAdd(xx, loc, base, reference));
                            }
                            case SUB -> {
                                var aopt = erb.mustEr(GenAspectOperatorSub.get(xx, loc, baseType));
                                if (aopt.optIsNone()) yield Opt.<CodeValue>none();
                                yield erb.mustEr(aopt.optUnwrap().aspectOperatorSubSub(xx, loc, base, reference));
                            }
                            case MUL -> {
                                var aopt = erb.mustEr(GenAspectOperatorMul.get(xx, loc, baseType));
                                if (aopt.optIsNone()) yield Opt.<CodeValue>none();
                                yield erb.mustEr(aopt.optUnwrap().aspectOperatorMulMul(xx, loc, base, reference));
                            }
                            case DIV -> {
                                var aopt = erb.mustEr(GenAspectOperatorDiv.get(xx, loc, baseType));
                                if (aopt.optIsNone()) yield Opt.<CodeValue>none();
                                yield erb.mustEr(aopt.optUnwrap().aspectOperatorDivDiv(xx, loc, base, reference));
                            }
                            case MOD -> {
                                var aopt = erb.mustEr(GenAspectOperatorMod.get(xx, loc, baseType));
                                if (aopt.optIsNone()) yield Opt.<CodeValue>none();
                                yield erb.mustEr(aopt.optUnwrap().aspectOperatorModMod(xx, loc, base, reference));
                            }
                            case AND -> {
                                var aopt = erb.mustEr(GenAspectOperatorAnd.get(xx, loc, baseType));
                                if (aopt.optIsNone()) yield Opt.<CodeValue>none();
                                yield erb.mustEr(aopt.optUnwrap().aspectOperatorAndAnd(xx, loc, base, reference));
                            }
                            case OR -> {
                                var aopt = erb.mustEr(GenAspectOperatorOr.get(xx, loc, baseType));
                                if (aopt.optIsNone()) yield Opt.<CodeValue>none();
                                yield erb.mustEr(aopt.optUnwrap().aspectOperatorOrOr(xx, loc, base, reference));
                            }
                            case XOR -> {
                                var aopt = erb.mustEr(GenAspectOperatorXor.get(xx, loc, baseType));
                                if (aopt.optIsNone()) yield Opt.<CodeValue>none();
                                yield erb.mustEr(aopt.optUnwrap().aspectOperatorXorXor(xx, loc, base, reference));
                            }
                            case SHL -> {
                                var aopt = erb.mustEr(GenAspectOperatorShl.get(xx, loc, baseType));
                                if (aopt.optIsNone()) yield Opt.<CodeValue>none();
                                yield erb.mustEr(aopt.optUnwrap().aspectOperatorShlShl(xx, loc, base, reference));
                            }
                            case SHR -> {
                                var aopt = erb.mustEr(GenAspectOperatorShr.get(xx, loc, baseType));
                                if (aopt.optIsNone()) yield Opt.<CodeValue>none();
                                yield erb.mustEr(aopt.optUnwrap().aspectOperatorShrShr(xx, loc, base, reference));
                            }
                            case USHR -> {
                                var aopt = erb.mustEr(GenAspectOperatorUshr.get(xx, loc, baseType));
                                if (aopt.optIsNone()) yield Opt.<CodeValue>none();
                                yield erb.mustEr(aopt.optUnwrap().aspectOperatorUshrUshr(xx, loc, base, reference));
                            }
                            case EQ -> {
                                var aopt = erb.mustEr(GenAspectOperatorEq.get(xx, loc, baseType));
                                if (aopt.optIsNone()) yield Opt.<CodeValue>none();
                                yield erb.mustEr(aopt.optUnwrap().aspectOperatorEqEq(xx, loc, base, reference));
                            }
                            case NE -> {
                                var aopt = erb.mustEr(GenAspectOperatorNe.get(xx, loc, baseType));
                                if (aopt.optIsNone()) yield Opt.<CodeValue>none();
                                yield erb.mustEr(aopt.optUnwrap().aspectOperatorNeNe(xx, loc, base, reference));
                            }
                            case LT -> {
                                var aopt = erb.mustEr(GenAspectOperatorLt.get(xx, loc, baseType));
                                if (aopt.optIsNone()) yield Opt.<CodeValue>none();
                                yield erb.mustEr(aopt.optUnwrap().aspectOperatorLtLt(xx, loc, base, reference));
                            }
                            case LE -> {
                                var aopt = erb.mustEr(GenAspectOperatorLe.get(xx, loc, baseType));
                                if (aopt.optIsNone()) yield Opt.<CodeValue>none();
                                yield erb.mustEr(aopt.optUnwrap().aspectOperatorLeLe(xx, loc, base, reference));
                            }
                            case GT -> {
                                var aopt = erb.mustEr(GenAspectOperatorGt.get(xx, loc, baseType));
                                if (aopt.optIsNone()) yield Opt.<CodeValue>none();
                                yield erb.mustEr(aopt.optUnwrap().aspectOperatorGtGt(xx, loc, base, reference));
                            }
                            case GE -> {
                                var aopt = erb.mustEr(GenAspectOperatorGe.get(xx, loc, baseType));
                                if (aopt.optIsNone()) yield Opt.<CodeValue>none();
                                yield erb.mustEr(aopt.optUnwrap().aspectOperatorGeGe(xx, loc, base, reference));
                            }
                        };
                if (out.optIsNone()) yield EResult.errs(erb.errors());
                var opOut = withPendingBlocks(xx, loc, erb, out.optUnwrap(), pendingBlocks);
                if (opOut.optIsNone()) yield EResult.errs(erb.errors());
                yield EResult.ok(EvalRes.of(opOut.optUnwrap(), xx.scope));
            }
            case Expr.OperatorUnary e -> {
                var erb = new Erb(loc);
                var bopt = erb.mustCv(xx, constEvalOperand(xx, e.base()));
                if (bopt.optIsNone()) yield EResult.errs(erb.errors());
                var base = bopt.optUnwrap();
                var bv = (Opt.Some<Value>) base.value();
                var baseType = bv.value().valueType();
                var out =
                        switch (e.op()) {
                            case NEG -> {
                                var aopt = erb.mustEr(GenAspectOperatorNeg.get(xx, loc, baseType));
                                if (aopt.optIsNone()) yield Opt.<CodeValue>none();
                                yield erb.mustEr(aopt.optUnwrap().aspectOperatorNegNeg(xx, loc, base));
                            }
                            case BITNOT -> {
                                var aopt = erb.mustEr(GenAspectOperatorBitNot.get(xx, loc, baseType));
                                if (aopt.optIsNone()) yield Opt.<CodeValue>none();
                                yield erb.mustEr(aopt.optUnwrap().aspectOperatorBitNotBitNot(xx, loc, base));
                            }
                        };
                if (out.optIsNone()) yield EResult.errs(erb.errors());
                yield EResult.ok(EvalRes.of(out.optUnwrap(), xx.scope));
            }
            case Expr.Foreach e -> {
                int baseLevelCount = xx.scope.levelCount();
                var level = xx.scope.pushMergeLabeled();
                EResult<EvalRes> res;
                foreachCompute:
                {
                    var erb = new Erb(loc);
                    var bopt = erb.mustCv(xx, constEvalOperand(xx, e.base()));
                    var ekopt = erb.mustCv(xx, constEvalOperand(xx, e.elementKey()));
                    if (bopt.optIsNone() || ekopt.optIsNone()) {
                        res = EResult.errs(erb.errors());
                        break foreachCompute;
                    }
                    var base = bopt.optUnwrap();
                    var ekCv = ekopt.optUnwrap();
                    var ekv = (Opt.Some<Value>) ekCv.value();
                    var ekOpt = erb.mustRes(Eval.extractConstKey(xx, ekv.value()));
                    if (ekOpt.optIsNone()) {
                        res = EResult.errs(erb.errors());
                        break foreachCompute;
                    }
                    var bv = (Opt.Some<Value>) base.value();
                    var baseType = bv.value().valueType();
                    var iterOpt = erb.mustEr(GenAspectIterable.get(xx, loc, baseType));
                    if (iterOpt.optIsNone()) {
                        res = EResult.errs(erb.errors());
                        break foreachCompute;
                    }
                    var iterable = iterOpt.optUnwrap();
                    var elementType = iterable.aspectIterableElementType();
                    var loopBinding = iterable.aspectIterableLoopBinding(xx);
                    var elemAccessRes = iterable.aspectIterableElementAccess(xx, loc, loopBinding);
                    if (elemAccessRes.hasErrors()) {
                        res = elemAccessRes.reerr();
                        break foreachCompute;
                    }
                    var elemBindAspectRes = GenAspectBind.get(xx, loc, elementType);
                    if (elemBindAspectRes.hasErrors()) {
                        res = elemBindAspectRes.reerr();
                        break foreachCompute;
                    }
                    var elemBindRes = elemBindAspectRes
                            .requireValue("Foreach: element AspectBind")
                            .aspectBindBind(xx, loc, elemAccessRes.requireValue("Foreach: element access"));
                    if (elemBindRes.hasErrors()) {
                        res = elemBindRes.reerr();
                        break foreachCompute;
                    }
                    var elemBind = elemBindRes.requireValue("Foreach: element bind");
                    var elementBinding = elemBind.binding();
                    var bodyScope = xx.scope.fork();
                    var pscId = bodyScope.vx.dropStateCounter.next();
                    var iterEntry = new ScopeLevel.Entry(
                            Opt.<ConstKey>none(),
                            loopBinding,
                            true,
                            pscId,
                            pscId,
                            new ScopeLevel.Emission(xx.vx.emissionCounter.next()));
                    bodyScope.levels.add(iterEntry);
                    var elemEntry = bodyScope.pushNamed(ekOpt.optUnwrap(), elementBinding);
                    var bodyXx = new ExprContext(bodyScope, xx.target, xx.vx, xx.importState, xx.currentExceptionType);
                    var bodyResult = constEval(bodyXx, e.body());
                    if (bodyResult.valueOpt().optIsNone()) {
                        bodyResult.drainInto(erb.errors());
                        res = EResult.errs(erb.errors());
                        break foreachCompute;
                    }
                    bodyResult.drainInto(erb.errors());
                    var bodyEv = bodyResult.requireValue("constEvalForeach body");
                    var bodyRanScope = bodyEv.scope();
                    var elemSpliceOpt = blocksSplice(xx, loc, elemEntry.scopeLevelEmission().blocks, erb);
                    if (elemSpliceOpt.optIsNone()) {
                        res = EResult.errs(erb.errors());
                        break foreachCompute;
                    }
                    var elemSplice = elemSpliceOpt.optUnwrap();
                    var iterSpliceOpt = blocksSplice(xx, loc, iterEntry.scopeLevelEmission().blocks, erb);
                    if (iterSpliceOpt.optIsNone()) {
                        res = EResult.errs(erb.errors());
                        break foreachCompute;
                    }
                    var iterSplice = iterSpliceOpt.optUnwrap();
                    popByBox(bodyRanScope, elemEntry);
                    popByBox(bodyRanScope, iterEntry);
                    var others = new TList<Scope>();
                    others.add(xx.scope);
                    var mergeRes = bodyRanScope.merge(xx, loc, others);
                    if (mergeRes.optIsSome()) {
                        res = EResult.errs(mergeRes.optUnwrap());
                        break foreachCompute;
                    }
                    xx.scope.adopt(bodyRanScope);
                    var out = erb.mustErMaybeUnreachable(
                            iterable.aspectIterableForeach(xx, loc, base, loopBinding, elemBind.code(), bodyEv.cv()));
                    if (out.optIsNone()) {
                        res = EResult.errs(erb.errors());
                        break foreachCompute;
                    }
                    var outCv = out.optUnwrap();
                    var outSeq = seqCode(xx, loc, erb, TList.of(outCv.code(), elemSplice, iterSplice));
                    if (outSeq.optIsNone()) {
                        res = EResult.errs(erb.errors());
                        break foreachCompute;
                    }
                    outCv = new CodeValue(outSeq.optUnwrap(), outCv.outcome());
                    var foreachRes = EvalRes.of(outCv, xx.scope);
                    res = erb.errors().isEmpty() ? EResult.ok(foreachRes) : EResult.mixed(foreachRes, erb.errors());
                }
                yield labeledConsume(xx, loc, level, baseLevelCount, res);
            }
            case Expr.Match e -> {
                int baseLevelCount = xx.scope.levelCount();
                var level = xx.scope.pushMergeLabeled();
                EResult<EvalRes> res;
                matchCompute:
                {
                    var erb = new Erb(loc);
                    var bopt = erb.mustCv(xx, constEvalOperand(xx, e.base()));
                    var ekopt = erb.mustCv(xx, constEvalOperand(xx, e.elementKey()));
                    if (bopt.optIsNone() || ekopt.optIsNone()) {
                        res = EResult.errs(erb.errors());
                        break matchCompute;
                    }
                    var base = bopt.optUnwrap();
                    var ekCv = ekopt.optUnwrap();
                    var ekv = (Opt.Some<Value>) ekCv.value();
                    var ekOpt = erb.mustRes(Eval.extractConstKey(xx, ekv.value()));
                    if (ekOpt.optIsNone()) {
                        res = EResult.errs(erb.errors());
                        break matchCompute;
                    }
                    var elementKey = ekOpt.optUnwrap();
                    var bv = (Opt.Some<Value>) base.value();
                    var baseType = bv.value().valueType();
                    var unionOpt = erb.mustEr(GenAspectUnion.get(xx, loc, baseType));
                    if (unionOpt.optIsNone()) {
                        res = EResult.errs(erb.errors());
                        break matchCompute;
                    }
                    var variants = unionOpt.optUnwrap().aspectUnionVariants();
                    var unionInst = TypeSourceUnionDynamic.unionOf(xx, baseType);
                    var active = new TList<Integer>();
                    if (unionInst.optIsSome()) {
                        active.addAll(unionInst.optUnwrap().active);
                    } else {
                        for (int i = 0; i < variants.size(); i++) active.add(i);
                    }
                    var caseTags = new TList<ConstKey>();
                    var caseIndices = new TList<Integer>();
                    for (var c : e.cases()) {
                        var condOpt = erb.mustCv(xx, constEvalOperand(xx, c.condition()));
                        if (condOpt.optIsNone()) {
                            res = EResult.errs(erb.errors());
                            break matchCompute;
                        }
                        var cv = (Opt.Some<Value>) condOpt.optUnwrap().value();
                        var tagOpt = erb.mustRes(Eval.extractConstKey(xx, cv.value()));
                        if (tagOpt.optIsNone()) {
                            res = EResult.errs(erb.errors());
                            break matchCompute;
                        }
                        var tag = tagOpt.optUnwrap();
                        int found = -1;
                        for (int j = 0; j < variants.size(); j++) {
                            if (variants.get(j).key().equals(tag)) {
                                found = j;
                                break;
                            }
                        }
                        if (found < 0) {
                            erb.errPush("Match case tag '" + tag + "' not found in union");
                            res = EResult.errs(erb.errors());
                            break matchCompute;
                        }
                        if (!active.contains(found)) {
                            erb.errPush("Match case tag '" + tag + "' is not an active variant of the matched value");
                            res = EResult.errs(erb.errors());
                            break matchCompute;
                        }
                        caseTags.add(tag);
                        caseIndices.add(found);
                    }
                    boolean exhaustive = true;
                    for (var idx : active) {
                        if (!caseIndices.contains(idx)) {
                            exhaustive = false;
                            break;
                        }
                    }
                    if (!exhaustive && e.fallback().optIsNone()) {
                        for (var idx : active) {
                            if (!caseIndices.contains(idx)) {
                                erb.errPush("Non-exhaustive match: missing case for tag '"
                                        + variants.get(idx).key() + "'");
                                res = EResult.errs(erb.errors());
                                break matchCompute;
                            }
                        }
                    }
                    if (exhaustive && e.fallback().optIsSome()) {
                        erb.errPush("Exhaustive match must not have a fallback");
                        res = EResult.errs(erb.errors());
                        break matchCompute;
                    }
                    var initialScope = xx.scope.fork();
                    var branchScopes = new TList<Scope>();
                    Type resultType = null;
                    boolean allControlEnded = true;
                    var matchCases = new TList<TargetCase>(e.cases().size());
                    for (int i = 0; i < e.cases().size(); i++) {
                        int variantIdx = caseIndices.get(i);
                        var variant = variants.get(variantIdx);
                        var caseScope = initialScope.fork();
                        var payloadRes = xx.target.targetInitialBind(xx, loc, variant.type());
                        if (payloadRes.hasErrors()) {
                            res = payloadRes.reerr();
                            break matchCompute;
                        }
                        var payloadBinding = payloadRes.requireValue("Match: payload initial bind");
                        var payloadEntry = caseScope.pushNamed(elementKey, payloadBinding);
                        var caseXx =
                                new ExprContext(caseScope, xx.target, xx.vx, xx.importState, xx.currentExceptionType);
                        var bodyResult = constEval(caseXx, e.cases().get(i).body());
                        if (bodyResult.valueOpt().optIsNone()) {
                            bodyResult.drainInto(erb.errors());
                            res = EResult.errs(erb.errors());
                            break matchCompute;
                        }
                        bodyResult.drainInto(erb.errors());
                        var bodyEv = bodyResult.requireValue("constEvalMatch case body");
                        var caseRanScope = bodyEv.scope();
                        var bodyCv = bodyEv.cv();
                        var payloadSpliceOpt = blocksSplice(caseXx, loc, payloadEntry.scopeLevelEmission().blocks, erb);
                        if (payloadSpliceOpt.optIsNone()) {
                            res = EResult.errs(erb.errors());
                            break matchCompute;
                        }
                        var payloadSplice = payloadSpliceOpt.optUnwrap();
                        var bodySeq = seqCode(xx, loc, erb, TList.of(bodyCv.code(), payloadSplice));
                        if (bodySeq.optIsNone()) {
                            res = EResult.errs(erb.errors());
                            break matchCompute;
                        }
                        bodyCv = new CodeValue(bodySeq.optUnwrap(), bodyCv.outcome());
                        popByBox(caseRanScope, payloadEntry);
                        branchScopes.add(caseRanScope);
                        if (bodyCv.value().optIsSome()) {
                            allControlEnded = false;
                            if (resultType == null) {
                                resultType = bodyCv.value().optUnwrap().valueType();
                            } else {
                                var convertedOpt = erb.mustEr(Eval.typecheckConvert(caseXx, loc, resultType, bodyCv));
                                if (convertedOpt.optIsNone()) {
                                    res = EResult.errs(erb.errors());
                                    break matchCompute;
                                }
                                bodyCv = convertedOpt.optUnwrap();
                            }
                        }
                        matchCases.add(new TargetCase(variant.type(), variantIdx, payloadBinding, bodyCv));
                    }
                    Opt<TargetMatchFallback> fallbackCv = Opt.none();
                    if (e.fallback().optIsSome()) {
                        var remaining = new TList<Integer>();
                        for (var idx : active) {
                            if (!caseIndices.contains(idx)) remaining.add(idx);
                        }
                        Type fallbackType =
                                unionInst.optIsSome() ? unionInst.optUnwrap().withActiveVariants(remaining) : baseType;
                        var fallbackScope = initialScope.fork();
                        var fallbackBindRes = xx.target.targetInitialBind(xx, loc, fallbackType);
                        if (fallbackBindRes.hasErrors()) {
                            res = fallbackBindRes.reerr();
                            break matchCompute;
                        }
                        var fallbackBinding = fallbackBindRes.requireValue("Match: fallback initial bind");
                        var fallbackEntry = fallbackScope.pushNamed(elementKey, fallbackBinding);
                        var fallbackXx = new ExprContext(
                                fallbackScope, xx.target, xx.vx, xx.importState, xx.currentExceptionType);
                        var fallbackResult = constEval(fallbackXx, e.fallback().optUnwrap());
                        if (fallbackResult.valueOpt().optIsNone()) {
                            fallbackResult.drainInto(erb.errors());
                            res = EResult.errs(erb.errors());
                            break matchCompute;
                        }
                        fallbackResult.drainInto(erb.errors());
                        var fallbackEv = fallbackResult.requireValue("constEvalMatch fallback");
                        var fallbackRanScope = fallbackEv.scope();
                        var fbCv = fallbackEv.cv();
                        var fallbackSpliceOpt =
                                blocksSplice(fallbackXx, loc, fallbackEntry.scopeLevelEmission().blocks, erb);
                        if (fallbackSpliceOpt.optIsNone()) {
                            res = EResult.errs(erb.errors());
                            break matchCompute;
                        }
                        var fallbackSplice = fallbackSpliceOpt.optUnwrap();
                        var fbSeq = seqCode(xx, loc, erb, TList.of(fbCv.code(), fallbackSplice));
                        if (fbSeq.optIsNone()) {
                            res = EResult.errs(erb.errors());
                            break matchCompute;
                        }
                        fbCv = new CodeValue(fbSeq.optUnwrap(), fbCv.outcome());
                        popByBox(fallbackRanScope, fallbackEntry);
                        branchScopes.add(fallbackRanScope);
                        if (fbCv.value().optIsSome()) {
                            allControlEnded = false;
                            if (resultType == null) {
                                resultType = fbCv.value().optUnwrap().valueType();
                            } else {
                                var convertedOpt = erb.mustEr(Eval.typecheckConvert(fallbackXx, loc, resultType, fbCv));
                                if (convertedOpt.optIsNone()) {
                                    res = EResult.errs(erb.errors());
                                    break matchCompute;
                                }
                                fbCv = convertedOpt.optUnwrap();
                            }
                        }
                        fallbackCv = Opt.some(new TargetMatchFallback(fallbackBinding, fbCv));
                    } else if (!allControlEnded) {
                        branchScopes.add(initialScope.fork());
                    }
                    if (branchScopes.isEmpty()) {
                        xx.scope.adopt(initialScope);
                    } else {
                        var merged = branchScopes.get(0);
                        var rest = new TList<Scope>(branchScopes.size() - 1);
                        for (int i = 1; i < branchScopes.size(); i++) rest.add(branchScopes.get(i));
                        var mergeRes = merged.merge(xx, loc, rest);
                        if (mergeRes.optIsSome()) {
                            res = EResult.errs(mergeRes.optUnwrap());
                            break matchCompute;
                        }
                        xx.scope.adopt(merged);
                    }
                    var out = erb.mustErMaybeUnreachable(xx.target.targetMatch(xx, loc, base, matchCases, fallbackCv));
                    if (out.optIsNone()) {
                        res = EResult.errs(erb.errors());
                        break matchCompute;
                    }
                    var outCv = out.optUnwrap();
                    var matchRes = EvalRes.of(outCv, xx.scope);
                    res = erb.errors().isEmpty() ? EResult.ok(matchRes) : EResult.mixed(matchRes, erb.errors());
                }
                yield labeledConsume(xx, loc, level, baseLevelCount, res);
            }
            case Expr.If e -> {
                int baseLevelCount = xx.scope.levelCount();
                var level = xx.scope.pushMergeLabeled();
                EResult<EvalRes> res;
                ifCompute:
                {
                    var erb = new Erb(loc);
                    var conditionsEvaluated = new TList<IfEval>();
                    boolean allConst = true;
                    for (var branch : e.conditions()) {
                        var condOpt = erb.mustCv(xx, constEvalOperand(xx, branch.condition()));
                        if (condOpt.optIsNone()) {
                            res = EResult.errs(erb.errors());
                            break ifCompute;
                        }
                        var cond = condOpt.optUnwrap();
                        var cv = (Opt.Some<Value>) cond.value();
                        var condType = cv.value().valueType();
                        var boolAspect = GenAspectBoolConst.getMaybe(xx, condType);
                        if (boolAspect.optIsSome()) {
                            var val = boolAspect.optUnwrap().aspectBoolConstGet(cv.value());
                            conditionsEvaluated.add(new IfEval(val, cond, branch.body()));
                        } else {
                            allConst = false;
                            conditionsEvaluated.add(new IfEval(false, cond, branch.body()));
                        }
                    }
                    if (allConst) {
                        var carriedParts = new TList<Code>();
                        for (var ie : conditionsEvaluated) {
                            carriedParts.add(ie.cond.code());
                            if (ie.val) {
                                var bodyResult = constEval(xx, ie.body);
                                if (bodyResult.valueOpt().optIsNone()) {
                                    bodyResult.drainInto(erb.errors());
                                    res = EResult.errs(erb.errors());
                                    break ifCompute;
                                }
                                bodyResult.drainInto(erb.errors());
                                var bodyEv = bodyResult.requireValue("constEvalIf const body");
                                var bodyOpt = e.else_().optIsSome()
                                        ? Opt.some(bodyEv.cv())
                                        : ifRequireNoValueWithoutElse(erb, bodyEv.cv());
                                if (bodyOpt.optIsNone()) {
                                    res = EResult.errs(erb.errors());
                                    break ifCompute;
                                }
                                var body = bodyOpt.optUnwrap();
                                carriedParts.add(body.code());
                                var bodySeq = seqCode(xx, loc, erb, carriedParts);
                                if (bodySeq.optIsNone()) {
                                    res = EResult.errs(erb.errors());
                                    break ifCompute;
                                }
                                var constRes =
                                        EvalRes.of(new CodeValue(bodySeq.optUnwrap(), body.outcome()), bodyEv.scope());
                                res = erb.errors().isEmpty()
                                        ? EResult.ok(constRes)
                                        : EResult.mixed(constRes, erb.errors());
                                break ifCompute;
                            }
                        }
                        if (e.else_().optIsSome()) {
                            var elseResult = constEval(xx, e.else_().optUnwrap());
                            if (elseResult.valueOpt().optIsNone()) {
                                elseResult.drainInto(erb.errors());
                                res = EResult.errs(erb.errors());
                                break ifCompute;
                            }
                            elseResult.drainInto(erb.errors());
                            var elseEv = elseResult.requireValue("constEvalIf const else");
                            var elseCv = elseEv.cv();
                            carriedParts.add(elseCv.code());
                            var elseSeq = seqCode(xx, loc, erb, carriedParts);
                            if (elseSeq.optIsNone()) {
                                res = EResult.errs(erb.errors());
                                break ifCompute;
                            }
                            var constRes =
                                    EvalRes.of(new CodeValue(elseSeq.optUnwrap(), elseCv.outcome()), elseEv.scope());
                            res = erb.errors().isEmpty() ? EResult.ok(constRes) : EResult.mixed(constRes, erb.errors());
                            break ifCompute;
                        }
                        var carriedSeq = seqCode(xx, loc, erb, carriedParts);
                        if (carriedSeq.optIsNone()) {
                            res = EResult.errs(erb.errors());
                            break ifCompute;
                        }
                        res = EResult.ok(
                                EvalRes.of(new CodeValue(carriedSeq.optUnwrap(), EvalOutcome.NO_VALUE), xx.scope));
                        break ifCompute;
                    }
                    var initialScope = xx.scope.fork();
                    var branchScopes = new TList<Scope>();
                    Type resultType = null;
                    var branches = new TList<IfBranch>(conditionsEvaluated.size());
                    for (var ie : conditionsEvaluated) {
                        var branchXx = new ExprContext(
                                initialScope.fork(), xx.target, xx.vx, xx.importState, xx.currentExceptionType);
                        var bodyResult = constEval(branchXx, ie.body);
                        if (bodyResult.valueOpt().optIsNone()) {
                            bodyResult.drainInto(erb.errors());
                            res = EResult.errs(erb.errors());
                            break ifCompute;
                        }
                        bodyResult.drainInto(erb.errors());
                        var bodyEv = bodyResult.requireValue("constEvalIf branch body");
                        branchScopes.add(bodyEv.scope());
                        var bodyCvOpt = e.else_().optIsSome()
                                ? Opt.some(bodyEv.cv())
                                : ifRequireNoValueWithoutElse(erb, bodyEv.cv());
                        if (bodyCvOpt.optIsNone()) {
                            res = EResult.errs(erb.errors());
                            break ifCompute;
                        }
                        var bodyCv = bodyCvOpt.optUnwrap();
                        if (bodyCv.value().optIsSome()) {
                            if (resultType == null) {
                                resultType = bodyCv.value().optUnwrap().valueType();
                            } else {
                                var convertedOpt = erb.mustEr(Eval.typecheckConvert(branchXx, loc, resultType, bodyCv));
                                if (convertedOpt.optIsNone()) {
                                    res = EResult.errs(erb.errors());
                                    break ifCompute;
                                }
                                bodyCv = convertedOpt.optUnwrap();
                            }
                        }
                        branches.add(new IfBranch(ie.cond, bodyCv));
                    }
                    Opt<CodeValue> elseCv = Opt.none();
                    if (e.else_().optIsSome()) {
                        var elseXx = new ExprContext(
                                initialScope.fork(), xx.target, xx.vx, xx.importState, xx.currentExceptionType);
                        var elseResult = constEval(elseXx, e.else_().optUnwrap());
                        if (elseResult.valueOpt().optIsNone()) {
                            elseResult.drainInto(erb.errors());
                            res = EResult.errs(erb.errors());
                            break ifCompute;
                        }
                        elseResult.drainInto(erb.errors());
                        var elseEv = elseResult.requireValue("constEvalIf else body");
                        branchScopes.add(elseEv.scope());
                        var eCv = elseEv.cv();
                        if (eCv.value().optIsSome()) {
                            if (resultType == null) {
                                resultType = eCv.value().optUnwrap().valueType();
                            } else {
                                var convertedOpt = erb.mustEr(Eval.typecheckConvert(elseXx, loc, resultType, eCv));
                                if (convertedOpt.optIsNone()) {
                                    res = EResult.errs(erb.errors());
                                    break ifCompute;
                                }
                                eCv = convertedOpt.optUnwrap();
                            }
                        }
                        elseCv = Opt.some(eCv);
                    } else {
                        branchScopes.add(initialScope.fork());
                    }
                    if (branchScopes.isEmpty()) {
                        xx.scope.adopt(initialScope);
                    } else {
                        var merged = branchScopes.get(0);
                        var rest = new TList<Scope>(branchScopes.size() - 1);
                        for (int i = 1; i < branchScopes.size(); i++) rest.add(branchScopes.get(i));
                        var mergeRes = merged.merge(xx, loc, rest);
                        if (mergeRes.optIsSome()) {
                            res = EResult.errs(mergeRes.optUnwrap());
                            break ifCompute;
                        }
                        xx.scope.adopt(merged);
                    }
                    var out = erb.mustErMaybeUnreachable(xx.target.targetIf(xx, loc, branches, elseCv));
                    if (out.optIsNone()) {
                        res = EResult.errs(erb.errors());
                        break ifCompute;
                    }
                    var outCv = out.optUnwrap();
                    var ifRes = EvalRes.of(outCv, xx.scope);
                    res = erb.errors().isEmpty() ? EResult.ok(ifRes) : EResult.mixed(ifRes, erb.errors());
                }
                yield labeledConsume(xx, loc, level, baseLevelCount, res);
            }
            case Expr.As e -> {
                var erb = new Erb(loc);
                var bopt = erb.mustCv(xx, constEvalOperand(xx, e.base()));
                var topt = erb.mustCv(xx, constEvalOperand(xx, e.target()));
                if (bopt.optIsNone() || topt.optIsNone()) yield EResult.errs(erb.errors());
                var base = bopt.optUnwrap();
                var target = topt.optUnwrap();
                var tv = (Opt.Some<Value>) target.value();
                var targetMeta = tv.value().valueType();
                var aspectTypeOpt = erb.mustEr(GenAspectType.get(xx, loc, targetMeta));
                if (aspectTypeOpt.optIsNone()) yield EResult.errs(erb.errors());
                var targetType = aspectTypeOpt.optUnwrap().aspectTypeType(tv.value());
                var converted = erb.mustEr(Eval.typecheckConvert(xx, loc, targetType, base));
                if (converted.optIsNone()) yield EResult.errs(erb.errors());
                yield EResult.ok(EvalRes.of(converted.optUnwrap(), xx.scope));
            }
            case Expr.Try e -> {
                if (e.catchClause().optIsNone() && e.finallyBlock().optIsNone()) {
                    yield EResult.err(loc, "Try requires at least one of catch/finally");
                }
                var erb = new Erb(loc);
                Opt<Type> bodyExcType;
                if (e.catchClause().optIsSome()) {
                    var catchObj = e.catchClause().optUnwrap();
                    var etCvOpt = erb.mustCv(xx, constEvalOperand(xx, catchObj.exceptionType()));
                    if (etCvOpt.optIsNone()) yield EResult.errs(erb.errors());
                    var etCv = etCvOpt.optUnwrap();
                    var etv = (Opt.Some<Value>) etCv.value();
                    var etMeta = etv.value().valueType();
                    var atOpt = erb.mustEr(GenAspectType.get(xx, loc, etMeta));
                    if (atOpt.optIsNone()) yield EResult.errs(erb.errors());
                    bodyExcType = Opt.some(atOpt.optUnwrap().aspectTypeType(etv.value()));
                } else {
                    bodyExcType = xx.currentExceptionType;
                }
                ScopeLevel.Finally finallyLevel = null;
                if (e.finallyBlock().optIsSome()) {
                    finallyLevel = new ScopeLevel.Finally(
                            new TList<>(), new ScopeLevel.Emission(xx.vx.emissionCounter.next()));
                    xx.scope.levels.add(finallyLevel);
                }
                ScopeLevel.Catch catchLevel = null;
                JumpKey handlerKey = null;
                if (e.catchClause().optIsSome()) {
                    handlerKey = new JumpKey();
                    catchLevel =
                            new ScopeLevel.Catch(handlerKey, new ScopeLevel.Emission(xx.vx.emissionCounter.next()));
                    xx.scope.levels.add(catchLevel);
                }
                var bodyXx = new ExprContext(xx.scope.fork(), xx.target, xx.vx, xx.importState, bodyExcType);
                var bodyResult = constEval(bodyXx, e.body());
                if (bodyResult.valueOpt().optIsNone()) {
                    bodyResult.drainInto(erb.errors());
                    yield EResult.errs(erb.errors());
                }
                bodyResult.drainInto(erb.errors());
                var bodyEv = bodyResult.requireValue("constEvalTry body");
                var bodyRanScope = bodyEv.scope();
                var bodyCv = bodyEv.cv();
                Type bodyType;
                if (bodyCv.value().optIsSome()) {
                    bodyType = bodyCv.value().optUnwrap().valueType();
                } else {
                    bodyType = null;
                }
                Opt<Value> resultValue = bodyCv.value();
                if (catchLevel != null) {
                    popByBox(xx.scope, catchLevel);
                    popByBox(bodyRanScope, catchLevel);
                }
                Opt<TryCatchClause> catch_ = Opt.none();
                if (e.catchClause().optIsSome()) {
                    var catchObj = e.catchClause().optUnwrap();
                    var bnCvOpt = erb.mustCv(xx, constEvalOperand(xx, catchObj.bindingName()));
                    if (bnCvOpt.optIsNone()) yield EResult.errs(erb.errors());
                    var bnCv = bnCvOpt.optUnwrap();
                    var bnv = (Opt.Some<Value>) bnCv.value();
                    var bnCkOpt = erb.mustRes(Eval.extractConstKey(xx, bnv.value()));
                    if (bnCkOpt.optIsNone()) yield EResult.errs(erb.errors());
                    var catchScope = xx.scope.fork();
                    var catchBindRes = xx.target.targetInitialBind(xx, loc, bodyExcType.optUnwrap());
                    if (catchBindRes.hasErrors()) yield catchBindRes.reerr();
                    var catchBinding = catchBindRes.requireValue("Try: catch initial bind");
                    var payloadEntry = catchScope.pushNamed(bnCkOpt.optUnwrap(), catchBinding);
                    var catchXx =
                            new ExprContext(catchScope, xx.target, xx.vx, xx.importState, xx.currentExceptionType);
                    var catchResult = constEval(catchXx, catchObj.body());
                    if (catchResult.valueOpt().optIsNone()) {
                        catchResult.drainInto(erb.errors());
                        yield EResult.errs(erb.errors());
                    }
                    catchResult.drainInto(erb.errors());
                    var catchEv = catchResult.requireValue("constEvalTry catch body");
                    var catchRanScope = catchEv.scope();
                    var catchCv = catchEv.cv();
                    var payloadSpliceOpt = blocksSplice(catchXx, loc, payloadEntry.scopeLevelEmission().blocks, erb);
                    if (payloadSpliceOpt.optIsNone()) yield EResult.errs(erb.errors());
                    var payloadSplice = payloadSpliceOpt.optUnwrap();
                    var catchSeq = seqCode(xx, loc, erb, TList.of(catchCv.code(), payloadSplice));
                    if (catchSeq.optIsNone()) yield EResult.errs(erb.errors());
                    catchCv = new CodeValue(catchSeq.optUnwrap(), catchCv.outcome());
                    popByBox(catchRanScope, payloadEntry);
                    var mergeOthers = new TList<Scope>();
                    mergeOthers.add(catchRanScope);
                    var mergeRes = bodyRanScope.merge(xx, loc, mergeOthers);
                    if (mergeRes.optIsSome()) yield EResult.errs(mergeRes.optUnwrap());
                    boolean bodyFalls = !bodyCv.controlEnded();
                    boolean catchFalls = !catchCv.controlEnded();
                    if (bodyFalls
                            && catchFalls
                            && bodyCv.value().optIsSome() != catchCv.value().optIsSome()) {
                        erb.errPush("try: the body and the catch must both yield a value or neither");
                        yield EResult.errs(erb.errors());
                    }
                    if (bodyType != null && catchCv.value().optIsSome()) {
                        var convOpt = erb.mustErMaybeUnreachable(Eval.typecheckConvert(xx, loc, bodyType, catchCv));
                        if (convOpt.optIsNone()) yield EResult.errs(erb.errors());
                        catchCv = convOpt.optUnwrap();
                    }
                    if (resultValue.optIsNone()) {
                        resultValue = catchCv.value();
                    }
                    catch_ = Opt.some(new TryCatchClause(catchBinding, bodyExcType.optUnwrap(), catchCv));
                }
                xx.scope.adopt(bodyRanScope);
                Opt<Code> finallyNormal = Opt.none();
                if (finallyLevel != null) {
                    popByBox(xx.scope, finallyLevel);
                    var finExpr = ((Opt.Some<Expr>) e.finallyBlock()).value();
                    var normalOpt = constEvalFinally(xx, loc, finExpr, erb);
                    if (normalOpt.optIsNone()) yield EResult.errs(erb.errors());
                    var emission = finallyLevel.scopeLevelEmission();
                    for (var pendingEntry : finallyLevel.pending) {
                        var siteXx = new ExprContext(
                                xx.scope.fork(), xx.target, xx.vx, xx.importState, xx.currentExceptionType);
                        var siteOpt = constEvalFinally(siteXx, loc, finExpr, erb);
                        if (siteOpt.optIsNone()) yield EResult.errs(erb.errors());
                        var key = pendingEntry.key();
                        EResult<Code> nodeRes;
                        if (key.label().optIsSome()) {
                            nodeRes = xx.target.targetExitTrampoline(
                                    xx,
                                    loc,
                                    pendingEntry.self(),
                                    key.argument(emission),
                                    siteOpt.optUnwrap(),
                                    key.next().optUnwrap());
                        } else {
                            nodeRes = xx.target.targetExcCleanup(
                                    xx, loc, pendingEntry.self(), siteOpt.optUnwrap(), key.next());
                        }
                        if (nodeRes.hasErrors()) {
                            nodeRes.drainInto(erb.errors());
                            yield EResult.errs(erb.errors());
                        }
                        emission.blocks.add(nodeRes.requireValue("ScopeChains finally block"));
                    }
                    finallyLevel.pending.clear();
                    finallyNormal = Opt.some(normalOpt.optUnwrap());
                }
                var tryOpt = erb.mustErMaybeUnreachable(xx.target.targetTryCatchFinally(
                        xx,
                        loc,
                        bodyCv,
                        catch_,
                        handlerKey == null ? Opt.<JumpKey>none() : Opt.some(handlerKey),
                        finallyNormal));
                if (tryOpt.optIsNone()) yield EResult.errs(erb.errors());
                var tryCv = tryOpt.optUnwrap();
                Code tryCode = tryCv.code();
                if (finallyLevel != null) {
                    var finSpliceOpt = blocksSplice(xx, loc, finallyLevel.scopeLevelEmission().blocks, erb);
                    if (finSpliceOpt.optIsNone()) yield EResult.errs(erb.errors());
                    var finSplice = finSpliceOpt.optUnwrap();
                    var finSeq = seqCode(xx, loc, erb, TList.of(tryCode, finSplice));
                    if (finSeq.optIsNone()) yield EResult.errs(erb.errors());
                    tryCode = finSeq.optUnwrap();
                }
                if (catchLevel != null) {
                    var catchSpliceOpt = blocksSplice(xx, loc, catchLevel.scopeLevelEmission().blocks, erb);
                    if (catchSpliceOpt.optIsNone()) yield EResult.errs(erb.errors());
                    var catchSplice = catchSpliceOpt.optUnwrap();
                    var catchSeq = seqCode(xx, loc, erb, TList.of(tryCode, catchSplice));
                    if (catchSeq.optIsNone()) yield EResult.errs(erb.errors());
                    tryCode = catchSeq.optUnwrap();
                }
                var innerCv = new CodeValue(tryCode, tryCv.outcome());
                var res = EvalRes.of(innerCv, xx.scope);
                yield erb.errors().isEmpty() ? EResult.ok(res) : EResult.mixed(res, erb.errors());
            }
            case Expr.Labeled e -> {
                int baseLevelCount = xx.scope.levelCount();
                var level = xx.scope.pushLabeled(Opt.some(e.label()));
                yield labeledConsume(xx, loc, level, baseLevelCount, constEval(xx, e.inner()));
            }
            case Expr.Return e -> {
                var erb = new Erb(loc);
                CodeValue valCv;
                if (e.value().optIsSome()) {
                    var vres = constEval(xx, e.value().optUnwrap());
                    if (vres.valueOpt().optIsNone()) yield vres;
                    vres.drainInto(erb.errors());
                    var vev = vres.requireValue("constEvalReturn value");
                    xx.scope.adopt(vev.scope());
                    valCv = vev.cv();
                } else {
                    valCv = new CodeValue(Code.none(loc), EvalOutcome.NO_VALUE);
                }
                valCv = xx.target.targetReturn(xx, loc, valCv);
                Opt<Value> retValue;
                switch (valCv.outcome()) {
                    case EvalOutcome.HasValue hv -> retValue = Opt.some(hv.value());
                    case EvalOutcome.ControlEnded ce -> {
                        var cv = new CodeValue(valCv.code(), EvalOutcome.CONTROL_ENDED);
                        var res = EvalRes.of(cv, xx.scope);
                        yield erb.errors().isEmpty() ? EResult.ok(res) : EResult.mixed(res, erb.errors());
                    }
                    case EvalOutcome.NoValue nv -> retValue = Opt.none();
                }
                ScopeLevel.Labeled target = null;
                for (int i = xx.scope.levels.size() - 1; i >= 0 && target == null; i--) {
                    var level = xx.scope.levels.get(i);
                    switch (level) {
                        case ScopeLevel.Labeled lb -> {
                            if (e.label().optIsSome()) {
                                if (lb.label.optIsSome()
                                        && lb.label.optUnwrap().equals(e.label().optUnwrap())) {
                                    target = lb;
                                }
                            } else if (lb.isBlock) {
                                target = lb;
                            }
                        }
                        case ScopeLevel.Entry entry -> {}
                        case ScopeLevel.Temp temp -> {}
                        case ScopeLevel.Catch catch_ -> {}
                        case ScopeLevel.Finally finally_ -> {}
                    }
                }
                if (target == null) {
                    erb.errPush(
                            e.label().optIsSome()
                                    ? "return: no enclosing scope with label '"
                                            + e.label().optUnwrap() + "'"
                                    : "return: no enclosing control scope");
                    yield EResult.errs(erb.errors());
                }
                Opt<Type> argument = Opt.none();
                if (retValue.optIsSome()) {
                    if (target.landing.resultType.optIsNone()) {
                        if (!target.landing.arrivals.isEmpty()) {
                            erb.errPush(
                                    "return: this return yields a value but an earlier return in the block does not");
                            yield EResult.errs(erb.errors());
                        }
                    } else {
                        var convOpt = erb.mustEr(
                                Eval.typecheckConvert(xx, loc, target.landing.resultType.optUnwrap(), valCv));
                        if (convOpt.optIsNone()) yield EResult.errs(erb.errors());
                        valCv = convOpt.optUnwrap();
                    }
                    var exitOpt0 = erb.mustEr(xx.target.targetExitValue(xx, loc, valCv));
                    if (exitOpt0.optIsNone()) yield EResult.errs(erb.errors());
                    var exitValue = exitOpt0.optUnwrap();
                    valCv = exitValue.cv();
                    switch (valCv.outcome()) {
                        case EvalOutcome.HasValue hv -> retValue = Opt.some(hv.value());
                        case EvalOutcome.NoValue nv -> throw new AssertionError("return conversion lost value");
                        case EvalOutcome.ControlEnded ce -> throw new AssertionError("return conversion lost value");
                    }
                    if (target.landing.resultType.optIsNone()) {
                        target.landing.resultType =
                                Opt.some(retValue.optUnwrap().valueType());
                        target.landing.carries = exitValue.carries();
                    } else if (target.landing.carries != exitValue.carries()) {
                        erb.errPush("return: this return and an earlier one yield a runtime and a compile-time value;"
                                + " cast the compile-time one to a dynamic type");
                        yield EResult.errs(erb.errors());
                    }
                    if (target.landing.carries) argument = target.landing.resultType;
                } else if (target.landing.resultType.optIsSome()) {
                    erb.errPush("return: this return yields no value but an earlier return in the block does");
                    yield EResult.errs(erb.errors());
                }
                EResult<JumpKey> destRes;
                exitDestCompute:
                {
                    var scopeLevels = xx.scope.levels;
                    var cleanups = new TList<ScopeLevel>();
                    boolean found = false;
                    for (int i = scopeLevels.size() - 1; i >= 0; i--) {
                        var level = scopeLevels.get(i);
                        if (level.scopeLevelEmission().id == target.scopeLevelEmission().id) {
                            found = true;
                            break;
                        }
                        switch (level) {
                            case ScopeLevel.Entry en -> cleanups.add(en);
                            case ScopeLevel.Temp tmp -> cleanups.add(tmp);
                            case ScopeLevel.Finally fin -> cleanups.add(fin);
                            case ScopeLevel.Labeled lb -> {}
                            case ScopeLevel.Catch ca -> {}
                        }
                    }
                    if (!found) throw new AssertionError("exitDest: target label level is not in scope");
                    var discriminator = target.label.optIsSome() ? target.label.optUnwrap() : "";
                    var next = target.end;
                    for (int ci = cleanups.size() - 1; ci >= 0; ci--) {
                        var resolved = ScopeChains.node(
                                xx,
                                loc,
                                cleanups.get(ci),
                                new ScopeLevel.DestKey(
                                        ScopeChains.levelDropState(cleanups.get(ci)),
                                        Opt.some(discriminator),
                                        Opt.some(next),
                                        cleanups.get(ci).scopeLevelEmission().argumentIntern(xx, argument)));
                        if (resolved.hasErrors()) {
                            destRes = resolved.reerr();
                            break exitDestCompute;
                        }
                        next = resolved.requireValue("ScopeChains.exitDest node")
                                .optUnwrap();
                    }
                    destRes = EResult.ok(next);
                }
                if (destRes.hasErrors()) {
                    destRes.drainInto(erb.errors());
                    yield EResult.errs(erb.errors());
                }
                var dest = destRes.requireValue("constEvalReturn dest");
                target.landing.arrivals.add(new ScopeLevel.Arrival(loc, retValue, xx.scope.fork()));
                var exitOpt = erb.mustEr(xx.target.targetExit(xx, loc, dest, argument, valCv));
                if (exitOpt.optIsNone()) yield EResult.errs(erb.errors());
                var cv = new CodeValue(exitOpt.optUnwrap(), EvalOutcome.CONTROL_ENDED);
                var res = EvalRes.of(cv, xx.scope);
                yield erb.errors().isEmpty() ? EResult.ok(res) : EResult.mixed(res, erb.errors());
            }
        };
    }

    private static EResult<CodeValue> elementResult(ExprContext xx, EResult<EvalRes> r) {
        var vOpt = r.valueOpt();
        if (vOpt.optIsNone()) return r.reerr();
        var v = vOpt.optUnwrap();
        xx.scope.adopt(v.scope());
        var errs = r.errorsOpt();
        if (errs.optIsNone() || errs.optUnwrap().isEmpty()) return EResult.ok(v.cv());
        var copied = new TList<ExprErr>();
        copied.addAll(errs.optUnwrap());
        return EResult.mixed(v.cv(), copied);
    }

    private static EResult<EvalRes> constEvalOperand(ExprContext xx, Expr expr) {
        int before = xx.scope.levelCount();
        var res = constEval(xx, expr);
        xx.scope.popToLevelCount(before);
        return res;
    }

    private static Opt<CodeValue> ifRequireNoValueWithoutElse(Erb erb, CodeValue cv) {
        if (cv.value().optIsNone()) return Opt.some(cv);
        erb.errPush("if: a branch yields a value but there is no else");
        return Opt.none();
    }

    private static Opt<Code> constEvalFinally(ExprContext xx, QExprId loc, Expr finExpr, Erb erb) {
        var finRes = constEvalOperand(xx, finExpr);
        if (finRes.valueOpt().optIsNone()) {
            finRes.drainInto(erb.errors());
            return Opt.none();
        }
        finRes.drainInto(erb.errors());
        var finEv = finRes.valueOpt().optUnwrap();
        xx.scope.adopt(finEv.scope());
        var finCv = finEv.cv();
        if (finCv.controlEnded()) {
            erb.errPush("finally: the block must fall through");
            return Opt.none();
        }
        if (finCv.value().optIsNone()) return Opt.some(finCv.code());
        var dropOpt = erb.mustEr(Eval.dropValue(xx, loc, finCv));
        if (dropOpt.optIsNone()) return Opt.none();
        var drop = dropOpt.optUnwrap();
        if (drop.codeIsNone()) return Opt.some(finCv.code());
        return erb.mustEr(xx.target.targetSeq(loc, TList.of(finCv.code(), drop)));
    }

    private static Opt<CodeValue> constEvalOperandPending(ExprContext xx, Erb erb, Expr expr) {
        var opt = erb.mustCv(xx, constEvalOperand(xx, expr));
        if (opt.optIsNone()) return opt;
        var cv = opt.optUnwrap();
        if (cv.value().optIsSome()) xx.scope.pushTemp(cv.value().optUnwrap());
        return opt;
    }

    private static TList<Code> operandPendingClose(ExprContext xx, int operandBase) {
        var blocks = new TList<Code>();
        for (int i = operandBase; i < xx.scope.levels.size(); i++) {
            switch (xx.scope.levels.get(i)) {
                case ScopeLevel.Temp temp -> blocks.addAll(temp.scopeLevelEmission().blocks);
                case ScopeLevel.Entry en ->
                    throw new AssertionError("pending operand close: level above the operand base is not a Temp");
                case ScopeLevel.Labeled lb ->
                    throw new AssertionError("pending operand close: level above the operand base is not a Temp");
                case ScopeLevel.Catch ca ->
                    throw new AssertionError("pending operand close: level above the operand base is not a Temp");
                case ScopeLevel.Finally fin ->
                    throw new AssertionError("pending operand close: level above the operand base is not a Temp");
            }
        }
        xx.scope.popToLevelCount(operandBase);
        return blocks;
    }

    private static Opt<CodeValue> withPendingBlocks(
            ExprContext xx, QExprId loc, Erb erb, CodeValue cv, TList<Code> blocks) {
        if (blocks.isEmpty()) return Opt.some(cv);
        var spliceOpt = blocksSplice(xx, loc, blocks, erb);
        if (spliceOpt.optIsNone()) return Opt.none();
        var splice = spliceOpt.optUnwrap();
        var code = seqCode(xx, loc, erb, TList.of(cv.code(), splice));
        if (code.optIsNone()) return Opt.none();
        return Opt.some(new CodeValue(code.optUnwrap(), cv.outcome()));
    }

    private static EResult<CodeValue> elementResultPending(ExprContext xx, EResult<EvalRes> r) {
        var res = elementResult(xx, r);
        if (res.valueOpt().optIsNone()) return res;
        var cv = res.valueOpt().optUnwrap();
        if (cv.value().optIsSome()) xx.scope.pushTemp(cv.value().optUnwrap());
        return res;
    }

    private static void popByBox(Scope scope, ScopeLevel level) {
        var top = scope.levels.get(scope.levels.size() - 1);
        if (top.scopeLevelEmission().id != level.scopeLevelEmission().id) {
            throw new AssertionError("scope level pop does not match the owning level");
        }
        scope.popLevel();
    }

    private static Opt<Code> seqCode(ExprContext xx, QExprId loc, Erb erb, TList<Code> parts) {
        return erb.mustEr(xx.target.targetSeq(loc, parts));
    }

    private static Opt<Code> blocksSplice(ExprContext xx, QExprId loc, TList<Code> blocks, Erb erb) {
        if (blocks.isEmpty()) return Opt.some(Code.none(loc));
        var skip = new JumpKey();
        var jumpOpt = erb.mustEr(xx.target.targetJump(loc, skip));
        var landOpt = erb.mustErMaybeUnreachable(xx.target.targetLand(xx, loc, skip, Opt.none()));
        if (jumpOpt.optIsNone() || landOpt.optIsNone()) return Opt.none();
        var parts = new TList<Code>();
        parts.add(jumpOpt.optUnwrap());
        parts.addAll(blocks);
        blocks.clear();
        parts.add(landOpt.optUnwrap().code());
        return seqCode(xx, loc, erb, parts);
    }

    private static EResult<EvalRes> labeledConsume(
            ExprContext xx, QExprId loc, ScopeLevel.Labeled level, int baseLevelCount, EResult<EvalRes> innerResult) {
        if (innerResult.valueOpt().optIsNone()) {
            xx.scope.popToLevelCount(baseLevelCount);
            return innerResult;
        }
        var erb = new Erb(loc);
        innerResult.drainInto(erb.errors());
        var inner = innerResult.requireValue("labeledConsume inner");
        xx.scope.adopt(inner.scope());
        var topLevelRaw = xx.scope.levels.get(xx.scope.levels.size() - 1);
        ScopeLevel.Labeled top;
        switch (topLevelRaw) {
            case ScopeLevel.Labeled lb -> top = lb;
            case ScopeLevel.Entry en -> throw new AssertionError("labeledConsume: consumer level is not on top");
            case ScopeLevel.Temp t -> throw new AssertionError("labeledConsume: consumer level is not on top");
            case ScopeLevel.Catch c -> throw new AssertionError("labeledConsume: consumer level is not on top");
            case ScopeLevel.Finally f -> throw new AssertionError("labeledConsume: consumer level is not on top");
        }
        if (top.scopeLevelEmission().id != level.scopeLevelEmission().id) {
            throw new AssertionError("labeledConsume: consumer level is not on top");
        }
        xx.scope.popLevel();
        var innerCv = inner.cv();
        var arrivals = level.landing.arrivals;
        if (!arrivals.isEmpty()
                && level.landing.resultType.optIsSome()
                && innerCv.value().optIsSome()) {
            var fallConvRes = Eval.typecheckConvert(xx, loc, level.landing.resultType.optUnwrap(), innerCv);
            if (fallConvRes.hasErrors()) {
                fallConvRes.drainInto(erb.errors());
                return EResult.errs(erb.errors());
            }
            innerCv = fallConvRes.requireValue("labeledConsume fallthrough convert");
        }
        var blocks = level.scopeLevelEmission().blocks;
        boolean fallsThrough =
                switch (innerCv.outcome()) {
                    case EvalOutcome.ControlEnded ce -> false;
                    case EvalOutcome.NoValue nv -> true;
                    case EvalOutcome.HasValue hv -> true;
                };
        if (!arrivals.isEmpty() && fallsThrough && innerCv.value().optIsSome()) {
            var exitRes = xx.target.targetExitValue(xx, loc, innerCv);
            if (exitRes.hasErrors()) {
                exitRes.drainInto(erb.errors());
                return EResult.errs(erb.errors());
            }
            var exitValue = exitRes.requireValue("labeledConsume fallthrough exit value");
            innerCv = exitValue.cv();
            if (exitValue.carries() != level.landing.carries) {
                erb.errPush("block: the end of the block and a return site yield a runtime and a compile-time"
                        + " value; cast the compile-time one to a dynamic type");
                return EResult.errs(erb.errors());
            }
        }
        Opt<Type> argument = !arrivals.isEmpty() && level.landing.carries ? level.landing.resultType : Opt.none();
        if (!arrivals.isEmpty()
                && fallsThrough
                && level.landing.resultType.optIsSome()
                && innerCv.value().optIsNone()) {
            erb.errPush("block: return sites yield a value but the end of the block is reachable without one");
            return EResult.errs(erb.errors());
        }
        if (!arrivals.isEmpty()
                && fallsThrough
                && level.landing.resultType.optIsNone()
                && innerCv.value().optIsSome()) {
            erb.errPush("block: return sites yield no value but the end of the block yields one");
            return EResult.errs(erb.errors());
        }
        if (!arrivals.isEmpty()) {
            var arrivalScopes = new TList<Scope>(arrivals.size());
            for (var a : arrivals) {
                a.scope().popToLevelCount(baseLevelCount);
                arrivalScopes.add(a.scope());
            }
            if (fallsThrough) {
                var mergeRes = xx.scope.merge(xx, loc, arrivalScopes);
                if (mergeRes.optIsSome()) {
                    if (mergeRes.optIsSome()) erb.errors().addAll(mergeRes.optUnwrap());
                    return EResult.errs(erb.errors());
                }
            } else {
                var first = arrivalScopes.get(0);
                var rest = new TList<Scope>(arrivalScopes.size() - 1);
                for (int i = 1; i < arrivalScopes.size(); i++) rest.add(arrivalScopes.get(i));
                var mergeRes = first.merge(xx, loc, rest);
                if (mergeRes.optIsSome()) {
                    if (mergeRes.optIsSome()) erb.errors().addAll(mergeRes.optUnwrap());
                    return EResult.errs(erb.errors());
                }
                xx.scope.adopt(first);
            }
        }
        var code = innerCv.code();
        Opt<Value> resultValue =
                arrivals.isEmpty() ? innerCv.value() : arrivals.get(0).value();
        if (!arrivals.isEmpty() || !blocks.isEmpty()) {
            var parts = new TList<Code>();
            if (fallsThrough && argument.optIsSome()) {
                var storeOpt = erb.mustEr(xx.target.targetExit(xx, loc, level.end, argument, innerCv));
                if (storeOpt.optIsNone()) return EResult.errs(erb.errors());
                parts.add(storeOpt.optUnwrap());
            } else {
                parts.add(code);
                if (fallsThrough && !blocks.isEmpty()) {
                    var jumpOpt = erb.mustEr(xx.target.targetJump(loc, level.end));
                    if (jumpOpt.optIsNone()) return EResult.errs(erb.errors());
                    parts.add(jumpOpt.optUnwrap());
                }
            }
            parts.addAll(blocks);
            blocks.clear();
            var landOpt = erb.mustErMaybeUnreachable(xx.target.targetLand(xx, loc, level.end, argument));
            if (landOpt.optIsNone()) return EResult.errs(erb.errors());
            parts.add(landOpt.optUnwrap().code());
            if (argument.optIsSome()) {
                resultValue = landOpt.optUnwrap().value();
            }
            var seqOpt = seqCode(xx, loc, erb, parts);
            if (seqOpt.optIsNone()) return EResult.errs(erb.errors());
            code = seqOpt.optUnwrap();
        }
        EvalOutcome outcome;
        if (resultValue.optIsSome()) {
            outcome = EvalOutcome.ofValue(resultValue.optUnwrap());
        } else if (!arrivals.isEmpty()) {
            outcome = EvalOutcome.NO_VALUE;
        } else {
            outcome = switch (innerCv.outcome()) {
                case EvalOutcome.ControlEnded ce -> EvalOutcome.CONTROL_ENDED;
                case EvalOutcome.NoValue nv -> EvalOutcome.NO_VALUE;
                case EvalOutcome.HasValue hv -> EvalOutcome.NO_VALUE;
            };
        }
        var cv = new CodeValue(code, outcome);
        var res = EvalRes.of(cv, xx.scope);
        if (erb.errors().isEmpty()) return EResult.ok(res);
        return EResult.mixed(res, erb.errors());
    }

    private record IfEval(boolean val, CodeValue cond, Expr body) {}

    private static void findMatchedLowersInner(Expr expr, int sd, int ld, TList<Expr> out) {
        switch (expr.variant()) {
            case Expr.Lower lo -> {
                int newLd = ld + 1;
                if (newLd - sd == 1) {
                    out.add(lo.expr());
                } else {
                    findMatchedLowersInner(lo.expr(), sd, newLd, out);
                }
            }
            case Expr.Stage st -> findMatchedLowersInner(st.expr(), sd + 1, ld, out);
            case Expr.Graft.Pending p -> findMatchedLowersInner(p.expr(), sd, ld, out);
            case Expr.Graft.Evaluated ignored -> {}
            case Expr.Literal ignored -> {}
            case Expr.ScopeUse ignored -> {}
            case Expr.Labeled lb -> findMatchedLowersInner(lb.inner(), sd, ld, out);
            case Expr.Return rt -> {
                if (rt.value().optIsSome()) findMatchedLowersInner(rt.value().optUnwrap(), sd, ld, out);
            }
            case Expr.As as -> {
                findMatchedLowersInner(as.base(), sd, ld, out);
                findMatchedLowersInner(as.target(), sd, ld, out);
            }
            case Expr.Call c -> {
                findMatchedLowersInner(c.func(), sd, ld, out);
                findMatchedLowersInner(c.arg(), sd, ld, out);
            }
            case Expr.Bind b -> {
                findMatchedLowersInner(b.base(), sd, ld, out);
                findMatchedLowersInner(b.value(), sd, ld, out);
            }
            case Expr.Set s -> {
                findMatchedLowersInner(s.base(), sd, ld, out);
                findMatchedLowersInner(s.value(), sd, ld, out);
            }
            case Expr.Field f -> {
                findMatchedLowersInner(f.base(), sd, ld, out);
                findMatchedLowersInner(f.key(), sd, ld, out);
            }
            case Expr.Seq sq -> {
                for (var sub : sq.exprs()) findMatchedLowersInner(sub, sd, ld, out);
            }
            case Expr.Tuple t -> {
                for (var el : t.elements()) findMatchedLowersInner(el, sd, ld, out);
            }
            case Expr.Record r -> {
                for (var entry : r.entries()) {
                    findMatchedLowersInner(entry.key(), sd, ld, out);
                    findMatchedLowersInner(entry.value(), sd, ld, out);
                }
            }
            case Expr.If ifv -> {
                for (var b : ifv.conditions()) {
                    findMatchedLowersInner(b.condition(), sd, ld, out);
                    findMatchedLowersInner(b.body(), sd, ld, out);
                }
                if (ifv.else_().optIsSome()) {
                    findMatchedLowersInner(ifv.else_().optUnwrap(), sd, ld, out);
                }
            }
            case Expr.OperatorBinary o -> {
                findMatchedLowersInner(o.base(), sd, ld, out);
                findMatchedLowersInner(o.reference(), sd, ld, out);
            }
            case Expr.OperatorUnary o -> findMatchedLowersInner(o.base(), sd, ld, out);
            case Expr.Foreach fe -> {
                findMatchedLowersInner(fe.base(), sd, ld, out);
                findMatchedLowersInner(fe.elementKey(), sd, ld, out);
                findMatchedLowersInner(fe.body(), sd, ld, out);
            }
            case Expr.Match m -> {
                findMatchedLowersInner(m.base(), sd, ld, out);
                findMatchedLowersInner(m.elementKey(), sd, ld, out);
                for (var c : m.cases()) {
                    findMatchedLowersInner(c.condition(), sd, ld, out);
                    findMatchedLowersInner(c.body(), sd, ld, out);
                }
                if (m.fallback().optIsSome()) {
                    findMatchedLowersInner(m.fallback().optUnwrap(), sd, ld, out);
                }
            }
            case Expr.Try tr -> {
                findMatchedLowersInner(tr.body(), sd, ld, out);
                if (tr.catchClause().optIsSome()) {
                    var cs = tr.catchClause().optUnwrap();
                    findMatchedLowersInner(cs.bindingName(), sd, ld, out);
                    findMatchedLowersInner(cs.exceptionType(), sd, ld, out);
                    findMatchedLowersInner(cs.body(), sd, ld, out);
                }
                if (tr.finallyBlock().optIsSome()) {
                    findMatchedLowersInner(tr.finallyBlock().optUnwrap(), sd, ld, out);
                }
            }
        }
    }

    private static void findMatchedGraftsInner(Expr expr, int sd, TList<Expr> out) {
        switch (expr.variant()) {
            case Expr.Graft.Pending p -> {
                if (sd == 0) out.add(p.expr());
                else findMatchedGraftsInner(p.expr(), sd, out);
            }
            case Expr.Graft.Evaluated ignored -> {}
            case Expr.Stage st -> findMatchedGraftsInner(st.expr(), sd + 1, out);
            case Expr.Lower lo -> findMatchedGraftsInner(lo.expr(), sd, out);
            case Expr.Literal ignored -> {}
            case Expr.ScopeUse ignored -> {}
            case Expr.Labeled lb -> findMatchedGraftsInner(lb.inner(), sd, out);
            case Expr.Return rt -> {
                if (rt.value().optIsSome()) findMatchedGraftsInner(rt.value().optUnwrap(), sd, out);
            }
            case Expr.As as -> {
                findMatchedGraftsInner(as.base(), sd, out);
                findMatchedGraftsInner(as.target(), sd, out);
            }
            case Expr.Call c -> {
                findMatchedGraftsInner(c.func(), sd, out);
                findMatchedGraftsInner(c.arg(), sd, out);
            }
            case Expr.Bind b -> {
                findMatchedGraftsInner(b.base(), sd, out);
                findMatchedGraftsInner(b.value(), sd, out);
            }
            case Expr.Set s -> {
                findMatchedGraftsInner(s.base(), sd, out);
                findMatchedGraftsInner(s.value(), sd, out);
            }
            case Expr.Field f -> {
                findMatchedGraftsInner(f.base(), sd, out);
                findMatchedGraftsInner(f.key(), sd, out);
            }
            case Expr.Seq sq -> {
                for (var sub : sq.exprs()) findMatchedGraftsInner(sub, sd, out);
            }
            case Expr.Tuple t -> {
                for (var el : t.elements()) findMatchedGraftsInner(el, sd, out);
            }
            case Expr.Record r -> {
                for (var entry : r.entries()) {
                    findMatchedGraftsInner(entry.key(), sd, out);
                    findMatchedGraftsInner(entry.value(), sd, out);
                }
            }
            case Expr.If ifv -> {
                for (var b : ifv.conditions()) {
                    findMatchedGraftsInner(b.condition(), sd, out);
                    findMatchedGraftsInner(b.body(), sd, out);
                }
                if (ifv.else_().optIsSome()) {
                    findMatchedGraftsInner(ifv.else_().optUnwrap(), sd, out);
                }
            }
            case Expr.OperatorBinary o -> {
                findMatchedGraftsInner(o.base(), sd, out);
                findMatchedGraftsInner(o.reference(), sd, out);
            }
            case Expr.OperatorUnary o -> findMatchedGraftsInner(o.base(), sd, out);
            case Expr.Foreach fe -> {
                findMatchedGraftsInner(fe.base(), sd, out);
                findMatchedGraftsInner(fe.elementKey(), sd, out);
                findMatchedGraftsInner(fe.body(), sd, out);
            }
            case Expr.Match m -> {
                findMatchedGraftsInner(m.base(), sd, out);
                findMatchedGraftsInner(m.elementKey(), sd, out);
                for (var c : m.cases()) {
                    findMatchedGraftsInner(c.condition(), sd, out);
                    findMatchedGraftsInner(c.body(), sd, out);
                }
                if (m.fallback().optIsSome()) {
                    findMatchedGraftsInner(m.fallback().optUnwrap(), sd, out);
                }
            }
            case Expr.Try tr -> {
                findMatchedGraftsInner(tr.body(), sd, out);
                if (tr.catchClause().optIsSome()) {
                    var cs = tr.catchClause().optUnwrap();
                    findMatchedGraftsInner(cs.bindingName(), sd, out);
                    findMatchedGraftsInner(cs.exceptionType(), sd, out);
                    findMatchedGraftsInner(cs.body(), sd, out);
                }
                if (tr.finallyBlock().optIsSome()) {
                    findMatchedGraftsInner(tr.finallyBlock().optUnwrap(), sd, out);
                }
            }
        }
    }
}
