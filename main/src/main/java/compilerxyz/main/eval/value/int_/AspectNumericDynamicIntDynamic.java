package compilerxyz.main.eval.value.int_;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.gen.AspectNumericDynamic;

public final class AspectNumericDynamicIntDynamic implements AspectNumericDynamic {
    public static final AspectNumericDynamicIntDynamic INSTANCE = new AspectNumericDynamicIntDynamic();

    private AspectNumericDynamicIntDynamic() {}

    @Override
    public NumericKind aspectNumericDynamicKind() {
        return NumericKind.INT;
    }
}
