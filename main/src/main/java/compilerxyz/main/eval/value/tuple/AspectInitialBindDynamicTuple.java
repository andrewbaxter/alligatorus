package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.binding.BindingValue;
import compilerxyz.main.gen.AspectInitialBindDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectInitialBindDynamicTuple(Tuple type) implements AspectInitialBindDynamic {

    @Override
    public Binding aspectInitialBindDynamicInitialBindDynamic(BindingId dynamicKey) {
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
        return new BindingTuple(scopeFields, Opt.some(dynamicKey));
    }
}
