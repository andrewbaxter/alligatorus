package compilerxyz.main.eval.value.ulong;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectNumericConst;
import compilerxyz.main.gen.Value;

public final class AspectNumericConstUlongConst implements AspectNumericConst {
    public static final AspectNumericConstUlongConst INSTANCE = new AspectNumericConstUlongConst();

    private AspectNumericConstUlongConst() {}

    @Override
    public Number aspectNumericConstGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv;
        throw new IllegalArgumentException(
                "TypeUlongConst::get: expected ulong ValueObjectConst, got " + value.getClass());
    }

    @Override
    public NumericKind aspectNumericConstKind() {
        return NumericKind.ULONG;
    }
}
