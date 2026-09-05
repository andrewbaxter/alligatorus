package compilerxyz.main.eval.value.float_;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.gen.AspectNumericDynamic;

public final class AspectNumericDynamicFloatDynamic implements AspectNumericDynamic {
    public static final AspectNumericDynamicFloatDynamic INSTANCE = new AspectNumericDynamicFloatDynamic();

    private AspectNumericDynamicFloatDynamic() {}

    @Override
    public NumericKind aspectNumericDynamicKind() {
        return NumericKind.FLOAT;
    }
}
