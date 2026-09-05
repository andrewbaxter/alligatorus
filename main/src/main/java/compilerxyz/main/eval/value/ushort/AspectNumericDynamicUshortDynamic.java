package compilerxyz.main.eval.value.ushort;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.gen.AspectNumericDynamic;

public final class AspectNumericDynamicUshortDynamic implements AspectNumericDynamic {
    public static final AspectNumericDynamicUshortDynamic INSTANCE = new AspectNumericDynamicUshortDynamic();

    private AspectNumericDynamicUshortDynamic() {}

    @Override
    public NumericKind aspectNumericDynamicKind() {
        return NumericKind.USHORT;
    }
}
