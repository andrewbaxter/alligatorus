package compilerxyz.main.eval.value.float_;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicUnboxed;

public final class AspectDynamicUnboxedFloatDynamicBox implements AspectDynamicUnboxed {
    public static final AspectDynamicUnboxedFloatDynamicBox INSTANCE = new AspectDynamicUnboxedFloatDynamicBox();

    private AspectDynamicUnboxedFloatDynamicBox() {}

    @Override
    public Type aspectDynamicUnboxedUnboxedForm() {
        return new TypeFloatDynamic();
    }
}
