package compilerxyz.gen;


import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Binding;

@BuiltinFull(uniqueId = 1021L, identityId = 4021L)
public interface AspectInitialBind extends Aspect {
  Binding aspectInitialBindInitialBind(BindingId dynamicKey);
}
