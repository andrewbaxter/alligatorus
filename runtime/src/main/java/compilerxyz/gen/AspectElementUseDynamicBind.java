package compilerxyz.gen;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.aspect.Aspect;

@BuiltinFull(uniqueId = 1040L, identityId = 4040L)
public interface AspectElementUseDynamicBind extends Aspect {
    Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey);
}
