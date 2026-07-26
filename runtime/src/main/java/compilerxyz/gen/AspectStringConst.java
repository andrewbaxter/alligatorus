package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;

@Builtin(uniqueId = 1009L, identityId = 4009L)
public interface AspectStringConst extends Aspect {
  String aspectStringConstGet(Value value);
}
