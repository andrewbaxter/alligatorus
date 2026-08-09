package compilerxyz.gen;


import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;

@BuiltinFull(uniqueId = 1010L, identityId = 4010L)
public interface AspectType extends Aspect {
  Type aspectTypeType(Value value);
}
