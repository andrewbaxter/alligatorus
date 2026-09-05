package compilerxyz.main.eval.value.long_;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.gen.AspectNumericDynamic;

public final class AspectNumericDynamicLongDynamic implements AspectNumericDynamic {
    public static final AspectNumericDynamicLongDynamic INSTANCE = new AspectNumericDynamicLongDynamic();

    private AspectNumericDynamicLongDynamic() {}

    @Override
    public NumericKind aspectNumericDynamicKind() {
        return NumericKind.LONG;
    }
}
