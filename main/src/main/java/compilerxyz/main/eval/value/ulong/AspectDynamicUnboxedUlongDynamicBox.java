package compilerxyz.main.eval.value.ulong;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicUnboxed;

public final class AspectDynamicUnboxedUlongDynamicBox implements AspectDynamicUnboxed {
    public static final AspectDynamicUnboxedUlongDynamicBox INSTANCE = new AspectDynamicUnboxedUlongDynamicBox();

    private AspectDynamicUnboxedUlongDynamicBox() {}

    @Override
    public Type aspectDynamicUnboxedUnboxedForm() {
        return new TypeUlongDynamic();
    }
}
