package compilerxyz.main.eval.value.float_;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectNumericConst;
import compilerxyz.main.gen.Value;

public final class AspectNumericConstFloatConst implements AspectNumericConst {
    public static final AspectNumericConstFloatConst INSTANCE = new AspectNumericConstFloatConst();

    private AspectNumericConstFloatConst() {}

    @Override
    public Number aspectNumericConstGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv;
        throw new IllegalArgumentException(
                "TypeFloatConst::get: expected float ValueObjectConst, got " + value.getClass());
    }

    @Override
    public NumericKind aspectNumericConstKind() {
        return NumericKind.FLOAT;
    }
}
