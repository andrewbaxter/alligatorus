package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.Value;

public final class AspectTypeTuple implements AspectType {
    public static final AspectTypeTuple INSTANCE = new AspectTypeTuple();

    private AspectTypeTuple() {}

    @Override
    public Type aspectTypeType(Value value) {
        if (!(value instanceof Tuple t)) {
            throw new IllegalArgumentException("Tuple::type_: expected Tuple value, got " + value.getClass());
        }
        return t.valueType();
    }
}
