package compilerxyz.main.eval.value.ushort;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicBoxed;

public final class AspectDynamicBoxedUshortDynamic implements AspectDynamicBoxed {
    public static final AspectDynamicBoxedUshortDynamic INSTANCE = new AspectDynamicBoxedUshortDynamic();

    private AspectDynamicBoxedUshortDynamic() {}

    @Override
    public Type aspectDynamicBoxedBoxedForm() {
        return new TypeUshortDynamicBox();
    }
}
