package compilerxyz.main.eval.value.ubyte;

import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectInt;
import compilerxyz.main.gen.Value;

public final class AspectIntUbyteConst implements AspectInt {
    public static final AspectIntUbyteConst INSTANCE = new AspectIntUbyteConst();

    private AspectIntUbyteConst() {}

    @Override
    public long aspectIntGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv.longValue();
        throw new IllegalArgumentException(
                "TypeUbyteConst::get: expected ubyte ValueObjectConst, got " + value.getClass());
    }
}
