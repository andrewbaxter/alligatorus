package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.gen.AspectInitialBindDynamic;
import compilerxyz.main.gen.Binding;

public record AspectInitialBindDynamicEResultDynamic<T extends Type>(TypeEResultDynamic<T> type)
        implements AspectInitialBindDynamic {

    @Override
    public Binding aspectInitialBindDynamicInitialBindDynamic(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, type);
    }
}
