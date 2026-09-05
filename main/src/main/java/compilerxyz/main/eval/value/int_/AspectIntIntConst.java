package compilerxyz.main.eval.value.int_;

import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectInt;
import compilerxyz.main.gen.Value;

public final class AspectIntIntConst implements AspectInt {
    public static final AspectIntIntConst INSTANCE = new AspectIntIntConst();

    private AspectIntIntConst() {}

    @Override
    public long aspectIntGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv.longValue();
        throw new IllegalArgumentException("TypeIntConst::get: expected int ValueObjectConst, got " + value.getClass());
    }
}
