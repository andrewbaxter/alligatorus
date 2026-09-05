package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.gen.AspectInitialBindDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectInitialBindDynamicTuple(Tuple type) implements AspectInitialBindDynamic {

    @Override
    public Binding aspectInitialBindDynamicInitialBindDynamic(BindingId dynamicKey) {
        var scopeFields = new TList<TupleScopeField>(type.elements.size());
        for (int i = 0; i < type.elements.size(); i++) {
            var e = type.elements.get(i);
            TupleElement.Dynamic d;
            switch (e) {
                case TupleElement.Dynamic dyn -> d = dyn;
                case TupleElement.EvalRes r ->
                    throw new IllegalStateException("Tuple::initialBind: element " + i + " is not Dynamic-only");
            }
            scopeFields.add(new TupleScopeField.Dynamic(d.type()));
        }
        return new BindingTuple(scopeFields, Opt.some(dynamicKey));
    }
}
