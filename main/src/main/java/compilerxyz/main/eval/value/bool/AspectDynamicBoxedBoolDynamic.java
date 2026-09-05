package compilerxyz.main.eval.value.bool;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicBoxed;

public final class AspectDynamicBoxedBoolDynamic implements AspectDynamicBoxed {
    public static final AspectDynamicBoxedBoolDynamic INSTANCE = new AspectDynamicBoxedBoolDynamic();

    private AspectDynamicBoxedBoolDynamic() {}

    @Override
    public Type aspectDynamicBoxedBoxedForm() {
        return new TypeBoolDynamicBox();
    }
}
