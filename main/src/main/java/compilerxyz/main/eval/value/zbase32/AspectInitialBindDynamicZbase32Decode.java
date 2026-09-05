package compilerxyz.main.eval.value.zbase32;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.gen.AspectInitialBindDynamic;
import compilerxyz.main.gen.Binding;

public record AspectInitialBindDynamicZbase32Decode(TypeZbase32Decode type) implements AspectInitialBindDynamic {

    @Override
    public Binding aspectInitialBindDynamicInitialBindDynamic(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, type);
    }
}
