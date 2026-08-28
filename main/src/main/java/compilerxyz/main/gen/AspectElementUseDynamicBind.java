package compilerxyz.main.gen;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1040L, identityId = 4040L)
public interface AspectElementUseDynamicBind extends Aspect {
    Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey);
}
