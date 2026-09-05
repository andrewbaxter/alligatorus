package compilerxyz.main.eval.value.long_;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectNumericConst;
import compilerxyz.main.gen.Value;

public final class AspectNumericConstLongConst implements AspectNumericConst {
    public static final AspectNumericConstLongConst INSTANCE = new AspectNumericConstLongConst();

    private AspectNumericConstLongConst() {}

    @Override
    public Number aspectNumericConstGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv;
        throw new IllegalArgumentException(
                "TypeLongConst::get: expected long ValueObjectConst, got " + value.getClass());
    }

    @Override
    public NumericKind aspectNumericConstKind() {
        return NumericKind.LONG;
    }
}
