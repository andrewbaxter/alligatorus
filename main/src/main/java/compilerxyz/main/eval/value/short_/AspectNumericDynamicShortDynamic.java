package compilerxyz.main.eval.value.short_;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.gen.AspectNumericDynamic;

public final class AspectNumericDynamicShortDynamic implements AspectNumericDynamic {
    public static final AspectNumericDynamicShortDynamic INSTANCE = new AspectNumericDynamicShortDynamic();

    private AspectNumericDynamicShortDynamic() {}

    @Override
    public NumericKind aspectNumericDynamicKind() {
        return NumericKind.SHORT;
    }
}
