package compilerxyz.main.eval.value.ubyte;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicBoxed;

public final class AspectDynamicBoxedUbyteDynamic implements AspectDynamicBoxed {
    public static final AspectDynamicBoxedUbyteDynamic INSTANCE = new AspectDynamicBoxedUbyteDynamic();

    private AspectDynamicBoxedUbyteDynamic() {}

    @Override
    public Type aspectDynamicBoxedBoxedForm() {
        return new TypeUbyteDynamicBox();
    }
}
