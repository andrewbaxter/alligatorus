package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.ExprSubErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.BindSubDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.TupleBindSlot;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectBindTuple(Tuple type) implements AspectBind {

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        var jvmTargetRes = DynUtil.jvmTarget(xx, loc);
        if (jvmTargetRes.hasErrors()) return jvmTargetRes.reerr();
        var jvmTarget = jvmTargetRes.requireValue("jvm target");
        if (cv.value().optIsNone()) {
            throw new IllegalStateException("Tuple::bindSub: CodeValue has no value");
        }
        var cvValue = cv.value().optUnwrap();
        var carry = cv.code();

        if (cvValue instanceof ValueDynamic vd) {
            var tupleType = MustCast.mustCast(Tuple.class, vd.type());
            var scopeFields = new TList<TupleScopeField>(tupleType.elements().size());
            var heldBindCodes = new TList<Code>();
            for (var e : tupleType.elements()) {
                switch (e) {
                    case TupleElement.Dynamic d -> scopeFields.add(new TupleScopeField.Dynamic(d.type()));
                    case TupleElement.EvalRes held -> {
                        if (held.result().hasErrors()) {
                            scopeFields.add(new TupleScopeField.Error(
                                    held.result().errorsOpt().optUnwrap()));
                            continue;
                        }
                        var heldCv = held.result().requireValue("Tuple::bindSub held element");
                        var heldBindRes = GenAspectBind.get(
                                xx, loc, heldCv.value().optUnwrap().valueType());
                        if (heldBindRes.hasErrors()) {
                            scopeFields.add(new TupleScopeField.Error(
                                    heldBindRes.errorsOpt().optUnwrap()));
                            continue;
                        }
                        var heldBound = heldBindRes
                                .requireValue("Tuple::bindSub held AspectBind")
                                .aspectBindBind(xx, loc, heldCv);
                        if (heldBound.hasErrors()) {
                            scopeFields.add(new TupleScopeField.Error(
                                    heldBound.errorsOpt().optUnwrap()));
                            continue;
                        }
                        var heldBinding = heldBound.requireValue("Tuple::bindSub held bind");
                        heldBindCodes.add(heldBinding.code());
                        scopeFields.add(new TupleScopeField.Scope(heldBinding.binding()));
                    }
                }
            }
            var subBuilder = new JavaBytecodeBuilder(loc);
            var carryAdd = subBuilder.addCode(loc, carry);
            if (carryAdd.optIsSome()) return EResult.errs(carryAdd.optUnwrap());
            var baseReg = DynUtil.sourceReg(subBuilder, vd.source());
            var sub = new BindSubDynamic(subBuilder.code(baseReg), tupleType);
            var bound = jvmTarget.bindSlot(loc, sub);
            heldBindCodes.add(Code.target(bound.code));
            var codeRes = xx.target.targetSeq(loc, heldBindCodes);
            if (codeRes.hasErrors()) return codeRes.reerr();
            return EResult.ok(new BindResult(
                    codeRes.requireValue("Tuple::bindSub runtime codes"),
                    new BindingTuple(scopeFields, Opt.some(bound.id))));
        }
        var tuple = MustCast.mustCast(Tuple.class, cvValue);

        TList<ExprErr> errors = null;
        record ElemBindResult(Opt<BindSubDynamic> targetSub, Opt<CodeValue> constCv) {}
        var bindResults = new TList<ElemBindResult>(tuple.elements().size());
        boolean hasTarget = false;
        for (int i = 0; i < tuple.elements().size(); i++) {
            var elem = tuple.elements().get(i);
            switch (elem) {
                case TupleElement.Dynamic d -> {
                    if (errors == null) errors = new TList<>();
                    errors.add(new ExprErr(
                            loc,
                            TList.of(new ExprSubErr.Opaque(
                                    "Tuple element " + i + " is dynamic-only, cannot bind directly"))));
                }
                case TupleElement.EvalRes elemRes -> {
                    var inner = elemRes.result();
                    if (inner.hasErrors()) {
                        if (errors == null) errors = new TList<>();
                        inner.drainInto(errors);
                        continue;
                    }
                    var elemCv = inner.requireValue("Tuple::bindSub element value");
                    var savedValue = elemCv.outcome();
                    var subRes = jvmTarget.maybeBindSub(xx, loc, elemCv);
                    if (subRes.hasErrors()) {
                        if (errors == null) errors = new TList<>();
                        subRes.drainInto(errors);
                        continue;
                    }
                    var subOpt = subRes.requireValue("Tuple::bindSub targetMaybeBindSub");
                    if (subOpt.optIsSome()) {
                        hasTarget = true;
                        bindResults.add(new ElemBindResult(Opt.some(subOpt.optUnwrap()), Opt.none()));
                    } else {
                        bindResults.add(
                                new ElemBindResult(Opt.none(), Opt.some(new CodeValue(elemCv.code(), savedValue))));
                    }
                }
            }
        }
        if (errors != null && !errors.isEmpty()) return EResult.errs(errors);
        var constBindCodes = new TList<Code>();
        if (!hasTarget) {
            var scopeFields = new TList<TupleScopeField>(bindResults.size());
            for (var br : bindResults) {
                if (br.constCv().optIsNone())
                    throw new IllegalStateException(
                            "Tuple::bindSub: !has_target but bindResults contains a target sub");
                var elemCv = br.constCv().optUnwrap();
                if (elemCv.value().optIsNone())
                    throw new IllegalStateException("Tuple::bindSub: const element CodeValue has no value");
                var elemType = elemCv.value().optUnwrap().valueType();
                var bindRes = GenAspectBind.get(xx, loc, elemType);
                if (bindRes.hasErrors()) {
                    scopeFields.add(
                            new TupleScopeField.Error(bindRes.errorsOpt().optUnwrap()));
                    continue;
                }
                var bind = bindRes.requireValue("Tuple::bindSub AspectBind");
                var elemBindRes = bind.aspectBindBind(xx, loc, elemCv);
                if (elemBindRes.hasErrors()) {
                    scopeFields.add(
                            new TupleScopeField.Error(elemBindRes.errorsOpt().optUnwrap()));
                    continue;
                }
                var elemBind = elemBindRes.requireValue("Tuple::bindSub element AspectBind.bind");
                constBindCodes.add(elemBind.code());
                scopeFields.add(new TupleScopeField.Scope(elemBind.binding()));
            }
            var codeRes = xx.target.targetSeq(loc, constBindCodes);
            if (codeRes.hasErrors()) return codeRes.reerr();
            return EResult.ok(new BindResult(
                    codeRes.requireValue("Tuple::bindSub const codes"), new BindingTuple(scopeFields, Opt.none())));
        }
        var scopeFields = new TList<TupleScopeField>(bindResults.size());
        var targetSlots = new TList<TupleBindSlot>(bindResults.size());
        var pendingConst = new TList<Code>();
        for (var br : bindResults) {
            if (br.targetSub().optIsSome()) {
                var targetSome = br.targetSub().optUnwrap();
                scopeFields.add(new TupleScopeField.Dynamic(targetSome.type()));
                var withPrefixRes = DynUtil.bindSubWithHeldCodes(xx, loc, targetSome, pendingConst, true);
                if (withPrefixRes.hasErrors()) return withPrefixRes.reerr();
                targetSlots.add(new TupleBindSlot(withPrefixRes.requireValue("Tuple::bindSub prefixed sub")));
            } else if (br.constCv().optIsSome()) {
                var elemCv = br.constCv().optUnwrap();
                if (elemCv.value().optIsNone())
                    throw new IllegalStateException("Tuple::bindSub: const element CodeValue has no value");
                var elemType = elemCv.value().optUnwrap().valueType();
                var bindRes = GenAspectBind.get(xx, loc, elemType);
                if (bindRes.hasErrors()) {
                    scopeFields.add(
                            new TupleScopeField.Error(bindRes.errorsOpt().optUnwrap()));
                    continue;
                }
                var bind = bindRes.requireValue("Tuple::bindSub AspectBind");
                var elemBindRes = bind.aspectBindBind(xx, loc, elemCv);
                if (elemBindRes.hasErrors()) {
                    scopeFields.add(
                            new TupleScopeField.Error(elemBindRes.errorsOpt().optUnwrap()));
                    continue;
                }
                var elemBind = elemBindRes.requireValue("Tuple::bindSub element AspectBind.bind");
                pendingConst.add(elemBind.code());
                scopeFields.add(new TupleScopeField.Scope(elemBind.binding()));
            } else {
                throw new IllegalStateException("Tuple::bindSub: bind result has neither target nor const");
            }
        }
        if (!pendingConst.isEmpty()) {
            int last = targetSlots.size() - 1;
            var withSuffixRes = DynUtil.bindSubWithHeldCodes(xx, loc, targetSlots.get(last).sub, pendingConst, false);
            if (withSuffixRes.hasErrors()) return withSuffixRes.reerr();
            targetSlots.set(last, new TupleBindSlot(withSuffixRes.requireValue("Tuple::bindSub suffixed sub")));
        }
        var combinedRes = jvmTarget.bindSubTuple(xx, loc, carry, targetSlots);
        if (combinedRes.hasErrors()) return combinedRes.reerr();
        var bound = jvmTarget.bindSlot(loc, combinedRes.requireValue("tuple bind sub"));
        return EResult.ok(new BindResult(Code.target(bound.code), new BindingTuple(scopeFields, Opt.some(bound.id))));
    }
}
