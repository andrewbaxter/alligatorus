package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1033L)
public interface AspectDynamicBoxed extends Aspect {
  Type aspectDynamicBoxedBoxedForm();
}
