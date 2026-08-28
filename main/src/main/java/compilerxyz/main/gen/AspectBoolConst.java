package compilerxyz.main.gen;

import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1006L, identityId = 4006L)
public interface AspectBoolConst extends Aspect {
    boolean aspectBoolConstGet(Value value);
}
