package compilerxyz.main.eval.value.double_;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicUnboxed;

public final class AspectDynamicUnboxedDoubleDynamicBox implements AspectDynamicUnboxed {
    public static final AspectDynamicUnboxedDoubleDynamicBox INSTANCE = new AspectDynamicUnboxedDoubleDynamicBox();

    private AspectDynamicUnboxedDoubleDynamicBox() {}

    @Override
    public Type aspectDynamicUnboxedUnboxedForm() {
        return new TypeDoubleDynamic();
    }
}
