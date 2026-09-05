package compilerxyz.main.eval.value.byte_;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.gen.AspectNumericDynamic;

public final class AspectNumericDynamicByteDynamic implements AspectNumericDynamic {
    public static final AspectNumericDynamicByteDynamic INSTANCE = new AspectNumericDynamicByteDynamic();

    private AspectNumericDynamicByteDynamic() {}

    @Override
    public NumericKind aspectNumericDynamicKind() {
        return NumericKind.BYTE;
    }
}
