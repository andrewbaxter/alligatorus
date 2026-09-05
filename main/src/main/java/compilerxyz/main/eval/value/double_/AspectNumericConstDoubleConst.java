package compilerxyz.main.eval.value.double_;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectNumericConst;
import compilerxyz.main.gen.Value;

public final class AspectNumericConstDoubleConst implements AspectNumericConst {
    public static final AspectNumericConstDoubleConst INSTANCE = new AspectNumericConstDoubleConst();

    private AspectNumericConstDoubleConst() {}

    @Override
    public Number aspectNumericConstGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv;
        throw new IllegalArgumentException(
                "TypeDoubleConst::get: expected double ValueObjectConst, got " + value.getClass());
    }

    @Override
    public NumericKind aspectNumericConstKind() {
        return NumericKind.DOUBLE;
    }
}
