package compilerxyz.main.eval.value.bool;

import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectBoolConst;
import compilerxyz.main.gen.Value;

public final class AspectBoolConstBoolConst implements AspectBoolConst {
    public static final AspectBoolConstBoolConst INSTANCE = new AspectBoolConstBoolConst();

    private AspectBoolConstBoolConst() {}

    @Override
    public boolean aspectBoolConstGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Boolean bv) return bv;
        throw new IllegalArgumentException(
                "TypeBoolConst::get: expected bool ValueObjectConst, got " + value.getClass());
    }
}
