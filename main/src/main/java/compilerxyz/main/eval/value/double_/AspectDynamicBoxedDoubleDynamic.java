package compilerxyz.main.eval.value.double_;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicBoxed;

public final class AspectDynamicBoxedDoubleDynamic implements AspectDynamicBoxed {
    public static final AspectDynamicBoxedDoubleDynamic INSTANCE = new AspectDynamicBoxedDoubleDynamic();

    private AspectDynamicBoxedDoubleDynamic() {}

    @Override
    public Type aspectDynamicBoxedBoxedForm() {
        return new TypeDoubleDynamicBox();
    }
}
