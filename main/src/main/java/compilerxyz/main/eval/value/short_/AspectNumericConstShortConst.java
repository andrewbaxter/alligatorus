package compilerxyz.main.eval.value.short_;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectNumericConst;
import compilerxyz.main.gen.Value;

public final class AspectNumericConstShortConst implements AspectNumericConst {
    public static final AspectNumericConstShortConst INSTANCE = new AspectNumericConstShortConst();

    private AspectNumericConstShortConst() {}

    @Override
    public Number aspectNumericConstGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv;
        throw new IllegalArgumentException(
                "TypeShortConst::get: expected short ValueObjectConst, got " + value.getClass());
    }

    @Override
    public NumericKind aspectNumericConstKind() {
        return NumericKind.SHORT;
    }
}
