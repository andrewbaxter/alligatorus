package compilerxyz.main.gen;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1021L, identityId = 4021L)
public interface AspectInitialBind extends Aspect {
    Binding aspectInitialBindInitialBind(BindingId dynamicKey);
}
