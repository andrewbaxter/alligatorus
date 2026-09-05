package compilerxyz.main.eval.value.short_;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicBoxed;

public final class AspectDynamicBoxedShortDynamic implements AspectDynamicBoxed {
    public static final AspectDynamicBoxedShortDynamic INSTANCE = new AspectDynamicBoxedShortDynamic();

    private AspectDynamicBoxedShortDynamic() {}

    @Override
    public Type aspectDynamicBoxedBoxedForm() {
        return new TypeShortDynamicBox();
    }
}
