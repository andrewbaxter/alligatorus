package compilerxyz.main.eval.value.float_;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicBoxed;

public final class AspectDynamicBoxedFloatDynamic implements AspectDynamicBoxed {
    public static final AspectDynamicBoxedFloatDynamic INSTANCE = new AspectDynamicBoxedFloatDynamic();

    private AspectDynamicBoxedFloatDynamic() {}

    @Override
    public Type aspectDynamicBoxedBoxedForm() {
        return new TypeFloatDynamicBox();
    }
}
