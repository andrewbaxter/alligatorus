package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.gen.AspectInitialBindDynamic;
import compilerxyz.main.gen.Binding;

public record AspectInitialBindDynamicObjectConst(TypeObjectConst type) implements AspectInitialBindDynamic {

    @Override
    public Binding aspectInitialBindDynamicInitialBindDynamic(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, type);
    }
}
