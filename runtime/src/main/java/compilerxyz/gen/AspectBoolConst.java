package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;

@Builtin(uniqueId = 1006L)
public interface AspectBoolConst extends Aspect {
  boolean aspectBoolConstGet(Value value);
}
