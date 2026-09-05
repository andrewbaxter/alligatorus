package compilerxyz.main.eval.value.double_;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.gen.AspectNumericDynamic;

public final class AspectNumericDynamicDoubleDynamic implements AspectNumericDynamic {
    public static final AspectNumericDynamicDoubleDynamic INSTANCE = new AspectNumericDynamicDoubleDynamic();

    private AspectNumericDynamicDoubleDynamic() {}

    @Override
    public NumericKind aspectNumericDynamicKind() {
        return NumericKind.DOUBLE;
    }
}
