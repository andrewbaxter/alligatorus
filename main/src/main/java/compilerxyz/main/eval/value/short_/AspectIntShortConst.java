package compilerxyz.main.eval.value.short_;

import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectInt;
import compilerxyz.main.gen.Value;

public final class AspectIntShortConst implements AspectInt {
    public static final AspectIntShortConst INSTANCE = new AspectIntShortConst();

    private AspectIntShortConst() {}

    @Override
    public long aspectIntGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv.longValue();
        throw new IllegalArgumentException(
                "TypeShortConst::get: expected short ValueObjectConst, got " + value.getClass());
    }
}
