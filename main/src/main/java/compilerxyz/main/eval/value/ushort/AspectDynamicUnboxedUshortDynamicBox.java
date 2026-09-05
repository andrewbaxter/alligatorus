package compilerxyz.main.eval.value.ushort;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicUnboxed;

public final class AspectDynamicUnboxedUshortDynamicBox implements AspectDynamicUnboxed {
    public static final AspectDynamicUnboxedUshortDynamicBox INSTANCE = new AspectDynamicUnboxedUshortDynamicBox();

    private AspectDynamicUnboxedUshortDynamicBox() {}

    @Override
    public Type aspectDynamicUnboxedUnboxedForm() {
        return new TypeUshortDynamic();
    }
}
