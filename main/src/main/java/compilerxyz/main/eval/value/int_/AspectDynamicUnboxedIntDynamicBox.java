package compilerxyz.main.eval.value.int_;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicUnboxed;

public final class AspectDynamicUnboxedIntDynamicBox implements AspectDynamicUnboxed {
    public static final AspectDynamicUnboxedIntDynamicBox INSTANCE = new AspectDynamicUnboxedIntDynamicBox();

    private AspectDynamicUnboxedIntDynamicBox() {}

    @Override
    public Type aspectDynamicUnboxedUnboxedForm() {
        return new TypeIntDynamic();
    }
}
