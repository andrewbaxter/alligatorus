package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.ExprSubErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.TupleBindSlot;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.TargetBindSub;
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
        if (!(cvValue instanceof Tuple tuple)) {
            throw new IllegalStateException("Tuple::bindSub: expected Tuple value, got " + cvValue.getClass());
        }
        var carry = cv.code();

        if (type.base.optIsSome()) {
            Type tupleType = type;
            var scopeFields = new TList<TupleScopeField>(type.elements.size());
            for (var e : type.elements) {
                TupleElement.Dynamic d;
                switch (e) {
                    case TupleElement.Dynamic dyn -> d = dyn;
                    case TupleElement.EvalRes r ->
                        throw new IllegalStateException("Tuple::bindSub: bind on non-type tuple");
                }
                scopeFields.add(new TupleScopeField.Dynamic(d.type()));
            }
            var baseRes = DynUtil.wrapWithCarry(
                    loc, carry, type.base.optUnwrap().source().valueDynamicSourceRealize(loc));
            if (baseRes.hasErrors()) return baseRes.reerr();
            var sub = new TargetBindSub(baseRes.requireValue("Tuple::bindSub base realize"), tupleType);
            var bound = jvmTarget.bindSlot(xx, loc, sub);
            return EResult.ok(
                    new BindResult(Code.target(bound.code), new BindingTuple(scopeFields, Opt.some(bound.id))));
        }

        TList<ExprErr> errors = null;
        record ElemBindResult(Opt<TargetBindSub> targetSub, Opt<CodeValue> constCv) {}
        var bindResults = new TList<ElemBindResult>(tuple.elements.size());
        boolean hasTarget = false;
        for (int i = 0; i < tuple.elements.size(); i++) {
            var elem = tuple.elements.get(i);
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
                scopeFields.add(new TupleScopeField.Dynamic(targetSome.type));
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
        var bound = jvmTarget.bindSlot(xx, loc, combinedRes.requireValue("tuple bind sub"));
        return EResult.ok(new BindResult(Code.target(bound.code), new BindingTuple(scopeFields, Opt.some(bound.id))));
    }
}
