package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.TList;

public record AspectTypeTuple(IList<AspectType> elementAspects) implements AspectType {

    @Override
    public Type aspectTypeType(Value value) {
        var tuple = MustCast.mustCast(Tuple.class, value);
        var elementTypes = new TList<TupleElement>(tuple.elements().size());
        for (int i = 0; i < tuple.elements().size(); i++) {
            TupleElement.EvalRes er;
            switch (tuple.elements().get(i)) {
                case TupleElement.EvalRes erVal -> er = erVal;
                case TupleElement.Dynamic d ->
                    throw new IllegalArgumentException("Tuple::type_: element " + i + " has no compile-time value");
            }
            var elementValue =
                    er.result().requireValue("Tuple::type_ element").value().optUnwrap();
            elementTypes.add(new TupleElement.Dynamic(elementAspects.get(i).aspectTypeType(elementValue)));
        }
        return new Tuple(IList.from(elementTypes));
    }
}
