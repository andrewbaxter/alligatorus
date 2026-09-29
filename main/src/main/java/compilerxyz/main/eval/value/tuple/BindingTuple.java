package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.ExprSubErr.Opaque;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.BindingDynamicSlot;
import compilerxyz.main.eval.dynamic.CompositeClassEmitter;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record BindingTuple(TList<TupleScopeField> elements, Opt<BindingId> jsKey)
        implements Binding, BindingDynamicSlot {
    @Override
    public BindingId bindingDynamicSlotKey() {
        if (jsKey.optIsNone()) {
            throw new IllegalStateException("BindingTuple: binding has no dynamic slot");
        }
        return jsKey.optUnwrap();
    }

    @Override
    public CodeValue bindingUse(ExprContext xx, QExprId loc, BindingSetter setter, Code code) {
        var resultElements = new TList<TupleElement>(elements.size());
        for (int i = 0; i < elements.size(); i++) {
            var e = elements.get(i);
            var elementSetter = new BindingSetterTupleElement(elements, i, jsKey, setter);
            TupleElement te =
                    switch (e) {
                        case TupleScopeField.Scope s ->
                            new TupleElement.EvalRes(
                                    EResult.ok(s.binding().bindingUse(xx, loc, elementSetter, Code.none(loc))));
                        case TupleScopeField.Dynamic d -> new TupleElement.Dynamic(d.type());
                        case TupleScopeField.Error er -> new TupleElement.EvalRes(EResult.errs(er.errors()));
                    };
            resultElements.add(te);
        }
        var tuple = new Tuple(IList.from(resultElements));
        if (jsKey.optIsNone()) return new CodeValue(code, EvalOutcome.ofValue(tuple));
        var dynamicTypes = new TList<Type>();
        for (var e : elements) {
            switch (e) {
                case TupleScopeField.Dynamic d -> dynamicTypes.add(d.type());
                case TupleScopeField.Scope held -> {}
                case TupleScopeField.Error err -> {}
            }
        }
        var desc = JavaDataDescriptor.fromInternalName(
                JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, Tuple.sortTupleTypes(xx, dynamicTypes))));
        return new CodeValue(
                code,
                EvalOutcome.ofValue(
                        new ValueDynamic(new ValueDynamicSource.Slot(jsKey.optUnwrap(), desc, setter), tuple)));
    }

    @Override
    public Binding bindingFork() {
        var forked = new TList<TupleScopeField>(elements.size());
        for (var e : elements) {
            switch (e) {
                case TupleScopeField.Scope s ->
                    forked.add(new TupleScopeField.Scope(s.binding().bindingFork()));
                case TupleScopeField.Dynamic d -> forked.add(d);
                case TupleScopeField.Error er -> forked.add(er);
            }
        }
        return new BindingTuple(forked, jsKey);
    }

    @Override
    public EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others) {
        for (var other : others) {
            if (!(other instanceof BindingTuple o)) {
                return EResult.err(loc, "cannot merge BindingTuple with a differently-typed binding across branches");
            }
            if (o.elements.size() != elements.size()) {
                return EResult.err(loc, "cannot merge BindingTuple with different element count across branches");
            }
        }
        var mergedElements = new TList<TupleScopeField>(elements.size());
        var errors = new TList<ExprErr>();
        for (int i = 0; i < elements.size(); i++) {
            var e = elements.get(i);
            switch (e) {
                case TupleScopeField.Scope s -> {
                    var otherSubs = new TList<Binding>(others.size());
                    boolean shapeOk = true;
                    for (var other : others) {
                        var oe = ((BindingTuple) other).elements.get(i);
                        switch (oe) {
                            case TupleScopeField.Scope os -> otherSubs.add(os.binding());
                            case TupleScopeField.Dynamic d -> shapeOk = false;
                            case TupleScopeField.Error er -> shapeOk = false;
                        }
                        if (!shapeOk) break;
                    }
                    if (!shapeOk) {
                        errors.add(new ExprErr(
                                loc,
                                TList.of(new Opaque("cannot merge BindingTuple: element " + i
                                        + " has a different variant across branches"))));
                        mergedElements.add(e);
                    } else {
                        var mres = s.binding().bindingMerge(xx, loc, otherSubs);
                        mres.drainInto(errors);
                        var mv = mres.valueOpt();
                        if (mv.optIsSome()) {
                            mergedElements.add(new TupleScopeField.Scope(mv.optUnwrap()));
                        } else {
                            mergedElements.add(e);
                        }
                    }
                }
                case TupleScopeField.Dynamic d -> {
                    mergeOthers:
                    for (var other : others) {
                        var oe = ((BindingTuple) other).elements.get(i);
                        switch (oe) {
                            case TupleScopeField.Dynamic ignored -> {}
                            case TupleScopeField.Scope s -> {
                                errors.add(new ExprErr(
                                        loc,
                                        TList.of(new Opaque("cannot merge BindingTuple: element " + i
                                                + " has a different variant across branches"))));
                                break mergeOthers;
                            }
                            case TupleScopeField.Error er -> {
                                errors.add(new ExprErr(
                                        loc,
                                        TList.of(new Opaque("cannot merge BindingTuple: element " + i
                                                + " has a different variant across branches"))));
                                break mergeOthers;
                            }
                        }
                    }
                    mergedElements.add(d);
                }
                case TupleScopeField.Error er -> mergedElements.add(er);
            }
        }
        if (!errors.isEmpty()) {
            return EResult.errs(errors);
        }
        return EResult.ok(new BindingTuple(mergedElements, jsKey));
    }

    @Override
    public EResult<Code> bindingDrop(ExprContext xx, QExprId loc) {
        return Binding.bindingDrop(xx, loc, this);
    }

    private record BindingSetterTupleElement(
            TList<TupleScopeField> elements, int index, Opt<BindingId> jsKey, BindingSetter parent)
            implements BindingSetter {
        @Override
        public boolean bindingSetterSet(ExprContext xx, QExprId loc, Binding newBinding, boolean dropModified) {
            var replaced = new TList<TupleScopeField>(elements.size());
            for (int i = 0; i < elements.size(); i++) {
                replaced.add(i == index ? new TupleScopeField.Scope(newBinding) : elements.get(i));
            }
            return parent.bindingSetterSet(xx, loc, new BindingTuple(replaced, jsKey), dropModified);
        }
    }
}
