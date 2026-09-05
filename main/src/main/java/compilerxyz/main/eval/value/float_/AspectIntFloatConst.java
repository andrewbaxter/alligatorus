package compilerxyz.main.eval.value.float_;

import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectInt;
import compilerxyz.main.gen.Value;

public final class AspectIntFloatConst implements AspectInt {
    public static final AspectIntFloatConst INSTANCE = new AspectIntFloatConst();

    private AspectIntFloatConst() {}

    @Override
    public long aspectIntGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv.longValue();
        throw new IllegalArgumentException(
                "TypeFloatConst::get: expected float ValueObjectConst, got " + value.getClass());
    }
}
