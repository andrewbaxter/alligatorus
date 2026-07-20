package compilerxyz.gen;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Binding;

@BuiltinAspect(
    uniqueId = 1040L,
    kind = "ElementUseDynamicBind",
    javadoc = "Creates the loop-variable Binding for an element read out of a homogeneous collection (foreach).")
@compilerxyz.annotations.BuiltinTypeId(7213L)
public interface AspectElementUseDynamicBind extends Aspect {
  Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey);
}
