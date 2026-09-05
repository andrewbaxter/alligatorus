package compilerxyz.main.eval.value.ubyte;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicUnboxed;

public final class AspectDynamicUnboxedUbyteDynamicBox implements AspectDynamicUnboxed {
    public static final AspectDynamicUnboxedUbyteDynamicBox INSTANCE = new AspectDynamicUnboxedUbyteDynamicBox();

    private AspectDynamicUnboxedUbyteDynamicBox() {}

    @Override
    public Type aspectDynamicUnboxedUnboxedForm() {
        return new TypeUbyteDynamic();
    }
}
