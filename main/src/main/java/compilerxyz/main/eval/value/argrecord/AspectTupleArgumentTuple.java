package compilerxyz.main.eval.value.argrecord;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.binding.BindingSetterConsumed;
import compilerxyz.main.gen.AspectTuple;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.TList;

public record AspectTupleArgumentTuple(ArgumentTuple type) implements AspectTuple {

    @Override
    public TList<Type> aspectTupleElementTypes() {
        var out = new TList<Type>(type.elementTypes().size());
        out.addAll(type.elementTypes());
        return out;
    }

    @Override
    public TList<EResult<CodeValue>> aspectTupleTakeElements(ExprContext xx, QExprId loc, Value value) {
        var tuple = MustCast.mustCast(ArgumentTuple.class, value);
        var out = new TList<EResult<CodeValue>>(tuple.elements().size());
        for (var element : tuple.elements()) {
            out.add(EResult.ok(element.bindingUse(xx, loc, new BindingSetterConsumed(), Code.none(loc))));
        }
        return out;
    }
}
