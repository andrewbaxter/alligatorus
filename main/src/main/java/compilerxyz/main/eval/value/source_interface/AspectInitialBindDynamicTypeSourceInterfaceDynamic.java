package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.gen.AspectInitialBindDynamic;
import compilerxyz.main.gen.Binding;

public record AspectInitialBindDynamicTypeSourceInterfaceDynamic(TypeTypeSourceInterfaceDynamic type)
        implements AspectInitialBindDynamic {

    @Override
    public Binding aspectInitialBindDynamicInitialBindDynamic(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, new TypeTypeSourceInterfaceDynamic(type.type()));
    }
}
