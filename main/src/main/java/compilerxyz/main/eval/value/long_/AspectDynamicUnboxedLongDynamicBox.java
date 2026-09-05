package compilerxyz.main.eval.value.long_;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicUnboxed;

public final class AspectDynamicUnboxedLongDynamicBox implements AspectDynamicUnboxed {
    public static final AspectDynamicUnboxedLongDynamicBox INSTANCE = new AspectDynamicUnboxedLongDynamicBox();

    private AspectDynamicUnboxedLongDynamicBox() {}

    @Override
    public Type aspectDynamicUnboxedUnboxedForm() {
        return new TypeLongDynamic();
    }
}
