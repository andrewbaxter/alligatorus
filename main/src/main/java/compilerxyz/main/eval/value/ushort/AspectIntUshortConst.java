package compilerxyz.main.eval.value.ushort;

import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectInt;
import compilerxyz.main.gen.Value;

public final class AspectIntUshortConst implements AspectInt {
    public static final AspectIntUshortConst INSTANCE = new AspectIntUshortConst();

    private AspectIntUshortConst() {}

    @Override
    public long aspectIntGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv.longValue();
        throw new IllegalArgumentException(
                "TypeUshortConst::get: expected ushort ValueObjectConst, got " + value.getClass());
    }
}
