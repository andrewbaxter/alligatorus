package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Binding;

@BuiltinAspect(
    uniqueId = 1021L,
    kind = "InitialBind",
    javadoc = "Creates bindings for pre-existing values.")
public interface AspectInitialBind extends Aspect {
  Binding aspectInitialBindInitialBind(BindingId dynamicKey);
}
