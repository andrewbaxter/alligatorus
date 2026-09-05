package compilerxyz.main.eval.value.uint;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectNumericConst;
import compilerxyz.main.gen.Value;

public final class AspectNumericConstUintConst implements AspectNumericConst {
    public static final AspectNumericConstUintConst INSTANCE = new AspectNumericConstUintConst();

    private AspectNumericConstUintConst() {}

    @Override
    public Number aspectNumericConstGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv;
        throw new IllegalArgumentException(
                "TypeUintConst::get: expected uint ValueObjectConst, got " + value.getClass());
    }

    @Override
    public NumericKind aspectNumericConstKind() {
        return NumericKind.UINT;
    }
}
