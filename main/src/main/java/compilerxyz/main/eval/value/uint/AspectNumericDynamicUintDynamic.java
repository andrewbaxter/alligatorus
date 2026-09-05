package compilerxyz.main.eval.value.uint;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.gen.AspectNumericDynamic;

public final class AspectNumericDynamicUintDynamic implements AspectNumericDynamic {
    public static final AspectNumericDynamicUintDynamic INSTANCE = new AspectNumericDynamicUintDynamic();

    private AspectNumericDynamicUintDynamic() {}

    @Override
    public NumericKind aspectNumericDynamicKind() {
        return NumericKind.UINT;
    }
}
