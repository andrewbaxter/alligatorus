package compilerxyz.gen;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;

@BuiltinFull(uniqueId = 1045L, identityId = 4043L)
public interface AspectByte extends Aspect {
  byte aspectByteGet(Value value);
}
