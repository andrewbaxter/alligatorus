package compilerxyz.main.eval.value.short_;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicUnboxed;

public final class AspectDynamicUnboxedShortDynamicBox implements AspectDynamicUnboxed {
    public static final AspectDynamicUnboxedShortDynamicBox INSTANCE = new AspectDynamicUnboxedShortDynamicBox();

    private AspectDynamicUnboxedShortDynamicBox() {}

    @Override
    public Type aspectDynamicUnboxedUnboxedForm() {
        return new TypeShortDynamic();
    }
}
