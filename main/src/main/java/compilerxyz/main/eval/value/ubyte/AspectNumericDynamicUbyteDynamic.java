package compilerxyz.main.eval.value.ubyte;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.gen.AspectNumericDynamic;

public final class AspectNumericDynamicUbyteDynamic implements AspectNumericDynamic {
    public static final AspectNumericDynamicUbyteDynamic INSTANCE = new AspectNumericDynamicUbyteDynamic();

    private AspectNumericDynamicUbyteDynamic() {}

    @Override
    public NumericKind aspectNumericDynamicKind() {
        return NumericKind.UBYTE;
    }
}
