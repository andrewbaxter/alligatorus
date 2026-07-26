package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;

@Builtin(uniqueId = 1045L, identityId = 4043L)
public interface AspectByte extends Aspect {
  byte aspectByteGet(Value value);
}
