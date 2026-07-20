package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Binding;

@Builtin(uniqueId = 1040L)
@compilerxyz.annotations.BuiltinTypeId(7213L)
public interface AspectElementUseDynamicBind extends Aspect {
  Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey);
}
