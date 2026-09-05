package compilerxyz.main.eval.value.ubyte;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectNumericConst;
import compilerxyz.main.gen.Value;

public final class AspectNumericConstUbyteConst implements AspectNumericConst {
    public static final AspectNumericConstUbyteConst INSTANCE = new AspectNumericConstUbyteConst();

    private AspectNumericConstUbyteConst() {}

    @Override
    public Number aspectNumericConstGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv;
        throw new IllegalArgumentException(
                "TypeUbyteConst::get: expected ubyte ValueObjectConst, got " + value.getClass());
    }

    @Override
    public NumericKind aspectNumericConstKind() {
        return NumericKind.UBYTE;
    }
}
