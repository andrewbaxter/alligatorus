package compilerxyz.main.eval.value.int_;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectNumericConst;
import compilerxyz.main.gen.Value;

public final class AspectNumericConstIntConst implements AspectNumericConst {
    public static final AspectNumericConstIntConst INSTANCE = new AspectNumericConstIntConst();

    private AspectNumericConstIntConst() {}

    @Override
    public Number aspectNumericConstGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv;
        throw new IllegalArgumentException("TypeIntConst::get: expected int ValueObjectConst, got " + value.getClass());
    }

    @Override
    public NumericKind aspectNumericConstKind() {
        return NumericKind.INT;
    }
}
