package compilerxyz.main.eval.value.byte_;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectNumericConst;
import compilerxyz.main.gen.Value;

public final class AspectNumericConstByteConst implements AspectNumericConst {
    public static final AspectNumericConstByteConst INSTANCE = new AspectNumericConstByteConst();

    private AspectNumericConstByteConst() {}

    @Override
    public Number aspectNumericConstGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv;
        throw new IllegalArgumentException(
                "TypeByteConst::get: expected byte ValueObjectConst, got " + value.getClass());
    }

    @Override
    public NumericKind aspectNumericConstKind() {
        return NumericKind.BYTE;
    }
}
