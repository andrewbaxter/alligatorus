package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.gen.AspectInitialBindDynamic;
import compilerxyz.main.gen.Binding;

public record AspectInitialBindDynamicMapConstructorConst(TypeMapConstructorConst type)
        implements AspectInitialBindDynamic {

    @Override
    public Binding aspectInitialBindDynamicInitialBindDynamic(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, type);
    }
}
