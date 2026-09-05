package compilerxyz.main.eval.value.double_;

import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectInt;
import compilerxyz.main.gen.Value;

public final class AspectIntDoubleConst implements AspectInt {
    public static final AspectIntDoubleConst INSTANCE = new AspectIntDoubleConst();

    private AspectIntDoubleConst() {}

    @Override
    public long aspectIntGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv.longValue();
        throw new IllegalArgumentException(
                "TypeDoubleConst::get: expected double ValueObjectConst, got " + value.getClass());
    }
}
