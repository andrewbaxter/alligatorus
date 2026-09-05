package compilerxyz.main.eval.value.uint;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicBoxed;

public final class AspectDynamicBoxedUintDynamic implements AspectDynamicBoxed {
    public static final AspectDynamicBoxedUintDynamic INSTANCE = new AspectDynamicBoxedUintDynamic();

    private AspectDynamicBoxedUintDynamic() {}

    @Override
    public Type aspectDynamicBoxedBoxedForm() {
        return new TypeUintDynamicBox();
    }
}
