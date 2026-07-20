package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Binding;

@Builtin(uniqueId = 1021L)
@compilerxyz.annotations.BuiltinTypeId(7218L)
public interface AspectInitialBind extends Aspect {
  Binding aspectInitialBindInitialBind(BindingId dynamicKey);
}
