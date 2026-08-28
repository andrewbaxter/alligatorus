package compilerxyz.main.gen;

import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1045L, identityId = 4043L)
public interface AspectByte extends Aspect {
    byte aspectByteGet(Value value);
}
