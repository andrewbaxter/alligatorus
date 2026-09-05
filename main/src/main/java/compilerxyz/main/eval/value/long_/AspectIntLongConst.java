package compilerxyz.main.eval.value.long_;

import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectInt;
import compilerxyz.main.gen.Value;

public final class AspectIntLongConst implements AspectInt {
    public static final AspectIntLongConst INSTANCE = new AspectIntLongConst();

    private AspectIntLongConst() {}

    @Override
    public long aspectIntGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv.longValue();
        throw new IllegalArgumentException(
                "TypeLongConst::get: expected long ValueObjectConst, got " + value.getClass());
    }
}
