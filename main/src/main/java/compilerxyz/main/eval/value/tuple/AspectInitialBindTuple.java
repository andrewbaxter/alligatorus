package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.eval.BindingSlot;
import compilerxyz.main.eval.BindingSlotDynamic;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.binding.BindingValue;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.Binding;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectInitialBindTuple(Tuple type) implements AspectInitialBind {

    @Override
    public EResult<Binding> aspectInitialBindInitialBind(ExprContext xx, QExprId loc, BindingSlot slot) {
        var dynamicKey = BindingSlotDynamic.idOf(slot);
        var scopeFields = new TList<TupleScopeField>(type.elements().size());
        for (int i = 0; i < type.elements().size(); i++) {
            var e = type.elements().get(i);
            scopeFields.add(
                    switch (e) {
                        case TupleElement.Dynamic d -> new TupleScopeField.Dynamic(d.type());
                        case TupleElement.EvalRes held ->
                            new TupleScopeField.Scope(new BindingValue(held.result()
                                    .requireValue("Tuple::initialBind held element")
                                    .value()
                                    .optUnwrap()));
                    });
        }
        return EResult.ok(new BindingTuple(scopeFields, Opt.some(dynamicKey)));
    }
}
