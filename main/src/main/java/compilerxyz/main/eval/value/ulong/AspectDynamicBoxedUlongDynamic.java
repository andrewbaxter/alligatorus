package compilerxyz.main.eval.value.ulong;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicBoxed;

public final class AspectDynamicBoxedUlongDynamic implements AspectDynamicBoxed {
    public static final AspectDynamicBoxedUlongDynamic INSTANCE = new AspectDynamicBoxedUlongDynamic();

    private AspectDynamicBoxedUlongDynamic() {}

    @Override
    public Type aspectDynamicBoxedBoxedForm() {
        return new TypeUlongDynamicBox();
    }
}
