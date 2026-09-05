package compilerxyz.main.eval.value.uint;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicUnboxed;

public final class AspectDynamicUnboxedUintDynamicBox implements AspectDynamicUnboxed {
    public static final AspectDynamicUnboxedUintDynamicBox INSTANCE = new AspectDynamicUnboxedUintDynamicBox();

    private AspectDynamicUnboxedUintDynamicBox() {}

    @Override
    public Type aspectDynamicUnboxedUnboxedForm() {
        return new TypeUintDynamic();
    }
}
