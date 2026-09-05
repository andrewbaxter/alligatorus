package compilerxyz.main.eval.value.ulong;

import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectInt;
import compilerxyz.main.gen.Value;

public final class AspectIntUlongConst implements AspectInt {
    public static final AspectIntUlongConst INSTANCE = new AspectIntUlongConst();

    private AspectIntUlongConst() {}

    @Override
    public long aspectIntGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv.longValue();
        throw new IllegalArgumentException(
                "TypeUlongConst::get: expected ulong ValueObjectConst, got " + value.getClass());
    }
}
