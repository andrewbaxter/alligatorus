package compilerxyz.main.eval.value.bool;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicUnboxed;

public final class AspectDynamicUnboxedBoolDynamicBox implements AspectDynamicUnboxed {
    public static final AspectDynamicUnboxedBoolDynamicBox INSTANCE = new AspectDynamicUnboxedBoolDynamicBox();

    private AspectDynamicUnboxedBoolDynamicBox() {}

    @Override
    public Type aspectDynamicUnboxedUnboxedForm() {
        return new TypeBoolDynamic();
    }
}
