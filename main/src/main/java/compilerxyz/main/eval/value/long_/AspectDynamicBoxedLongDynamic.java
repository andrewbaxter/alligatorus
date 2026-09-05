package compilerxyz.main.eval.value.long_;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicBoxed;

public final class AspectDynamicBoxedLongDynamic implements AspectDynamicBoxed {
    public static final AspectDynamicBoxedLongDynamic INSTANCE = new AspectDynamicBoxedLongDynamic();

    private AspectDynamicBoxedLongDynamic() {}

    @Override
    public Type aspectDynamicBoxedBoxedForm() {
        return new TypeLongDynamicBox();
    }
}
