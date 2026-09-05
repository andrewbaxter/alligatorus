package compilerxyz.main.eval.value.byte_;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicBoxed;

public final class AspectDynamicBoxedByteDynamic implements AspectDynamicBoxed {
    public static final AspectDynamicBoxedByteDynamic INSTANCE = new AspectDynamicBoxedByteDynamic();

    private AspectDynamicBoxedByteDynamic() {}

    @Override
    public Type aspectDynamicBoxedBoxedForm() {
        return new TypeByteDynamicBox();
    }
}
