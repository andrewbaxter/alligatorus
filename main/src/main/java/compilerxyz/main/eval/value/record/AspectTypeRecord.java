package compilerxyz.main.eval.value.record;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.Value;

public final class AspectTypeRecord implements AspectType {
    public static final AspectTypeRecord INSTANCE = new AspectTypeRecord();

    private AspectTypeRecord() {}

    @Override
    public Type aspectTypeType(Value value) {
        if (!(value instanceof Record r)) {
            throw new IllegalArgumentException("Record::type_: expected Record value, got " + value.getClass());
        }
        return r.valueType();
    }
}
