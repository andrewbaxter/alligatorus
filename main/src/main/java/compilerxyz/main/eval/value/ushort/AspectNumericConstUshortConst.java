package compilerxyz.main.eval.value.ushort;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectNumericConst;
import compilerxyz.main.gen.Value;

public final class AspectNumericConstUshortConst implements AspectNumericConst {
    public static final AspectNumericConstUshortConst INSTANCE = new AspectNumericConstUshortConst();

    private AspectNumericConstUshortConst() {}

    @Override
    public Number aspectNumericConstGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv;
        throw new IllegalArgumentException(
                "TypeUshortConst::get: expected ushort ValueObjectConst, got " + value.getClass());
    }

    @Override
    public NumericKind aspectNumericConstKind() {
        return NumericKind.USHORT;
    }
}
