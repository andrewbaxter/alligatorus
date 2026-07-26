package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1034L, identityId = 4034L)
public interface AspectDynamicUnboxed extends Aspect {
  Type aspectDynamicUnboxedUnboxedForm();
}
