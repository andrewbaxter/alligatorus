package compilerxyz.main.eval.value.byte_;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicUnboxed;

public final class AspectDynamicUnboxedByteDynamicBox implements AspectDynamicUnboxed {
    public static final AspectDynamicUnboxedByteDynamicBox INSTANCE = new AspectDynamicUnboxedByteDynamicBox();

    private AspectDynamicUnboxedByteDynamicBox() {}

    @Override
    public Type aspectDynamicUnboxedUnboxedForm() {
        return new TypeByteDynamic();
    }
}
