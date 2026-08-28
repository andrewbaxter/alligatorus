package compilerxyz.gen;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.aspect.Aspect;

@BuiltinFull(uniqueId = 1006L, identityId = 4006L)
public interface AspectBoolConst extends Aspect {
    boolean aspectBoolConstGet(Value value);
}
