package compilerxyz.main.eval.value.byte_;

import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectInt;
import compilerxyz.main.gen.Value;

public final class AspectIntByteConst implements AspectInt {
    public static final AspectIntByteConst INSTANCE = new AspectIntByteConst();

    private AspectIntByteConst() {}

    @Override
    public long aspectIntGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv.longValue();
        throw new IllegalArgumentException(
                "TypeByteConst::get: expected byte ValueObjectConst, got " + value.getClass());
    }
}
