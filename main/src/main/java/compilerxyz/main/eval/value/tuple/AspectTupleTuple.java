package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectTuple;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.TList;

public record AspectTupleTuple(Tuple type) implements AspectTuple {

    @Override
    public TList<Type> aspectTupleElementTypes() {
        var out = new TList<Type>(type.elements().size());
        for (var e : type.elements()) {
            TupleElement.Dynamic d;
            switch (e) {
                case TupleElement.Dynamic dyn -> d = dyn;
                case TupleElement.EvalRes r ->
                    throw new IllegalStateException("Tuple::elementTypes: slot is not Dynamic-only");
            }
            out.add(d.type());
        }
        return out;
    }

    @Override
    public TList<EResult<CodeValue>> aspectTupleTakeElements(ExprContext xx, QExprId loc, Value value) {
        if (!(value instanceof Tuple t)) {
            throw new IllegalArgumentException("Tuple::takeElements: expected Tuple value, got " + value.getClass());
        }
        var out = new TList<EResult<CodeValue>>(t.elements().size());
        for (var e : t.elements()) {
            TupleElement.EvalRes r;
            switch (e) {
                case TupleElement.EvalRes res -> r = res;
                case TupleElement.Dynamic d ->
                    throw new IllegalStateException("Tuple::takeElements: slot is not EvalRes-form");
            }
            out.add(r.result());
        }
        return out;
    }
}
