package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.gen.AspectInitialBindDynamic;
import compilerxyz.main.gen.Binding;

public record AspectInitialBindDynamicMapDynamic<K extends Type, V extends Type>(TypeMapDynamic<K, V> type)
        implements AspectInitialBindDynamic {

    @Override
    public Binding aspectInitialBindDynamicInitialBindDynamic(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, type);
    }
}
