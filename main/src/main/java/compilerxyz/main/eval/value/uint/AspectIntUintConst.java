package compilerxyz.main.eval.value.uint;

import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectInt;
import compilerxyz.main.gen.Value;

public final class AspectIntUintConst implements AspectInt {
    public static final AspectIntUintConst INSTANCE = new AspectIntUintConst();

    private AspectIntUintConst() {}

    @Override
    public long aspectIntGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv.longValue();
        throw new IllegalArgumentException(
                "TypeUintConst::get: expected uint ValueObjectConst, got " + value.getClass());
    }
}
