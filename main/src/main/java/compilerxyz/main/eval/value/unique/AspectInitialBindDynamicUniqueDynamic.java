package compilerxyz.main.eval.value.unique;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.gen.AspectInitialBindDynamic;
import compilerxyz.main.gen.Binding;

public final class AspectInitialBindDynamicUniqueDynamic implements AspectInitialBindDynamic {
    public static final AspectInitialBindDynamicUniqueDynamic INSTANCE = new AspectInitialBindDynamicUniqueDynamic();

    private AspectInitialBindDynamicUniqueDynamic() {}

    @Override
    public Binding aspectInitialBindDynamicInitialBindDynamic(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, new TypeUniqueDynamic());
    }
}
