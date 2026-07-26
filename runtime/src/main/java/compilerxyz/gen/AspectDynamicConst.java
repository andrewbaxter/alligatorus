package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1048L, identityId = 4046L)
public interface AspectDynamicConst extends Aspect {
  Type aspectDynamicConstConstForm();
}
