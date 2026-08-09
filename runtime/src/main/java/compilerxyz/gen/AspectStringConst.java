package compilerxyz.gen;


import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;

@BuiltinFull(uniqueId = 1009L, identityId = 4009L)
public interface AspectStringConst extends Aspect {
  String aspectStringConstGet(Value value);
}
