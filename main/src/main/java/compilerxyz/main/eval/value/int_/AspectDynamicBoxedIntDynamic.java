package compilerxyz.main.eval.value.int_;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicBoxed;

public final class AspectDynamicBoxedIntDynamic implements AspectDynamicBoxed {
    public static final AspectDynamicBoxedIntDynamic INSTANCE = new AspectDynamicBoxedIntDynamic();

    private AspectDynamicBoxedIntDynamic() {}

    @Override
    public Type aspectDynamicBoxedBoxedForm() {
        return new TypeIntDynamicBox();
    }
}
