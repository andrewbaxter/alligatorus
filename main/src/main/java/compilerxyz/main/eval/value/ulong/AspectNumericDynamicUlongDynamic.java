package compilerxyz.main.eval.value.ulong;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.gen.AspectNumericDynamic;

public final class AspectNumericDynamicUlongDynamic implements AspectNumericDynamic {
    public static final AspectNumericDynamicUlongDynamic INSTANCE = new AspectNumericDynamicUlongDynamic();

    private AspectNumericDynamicUlongDynamic() {}

    @Override
    public NumericKind aspectNumericDynamicKind() {
        return NumericKind.ULONG;
    }
}
