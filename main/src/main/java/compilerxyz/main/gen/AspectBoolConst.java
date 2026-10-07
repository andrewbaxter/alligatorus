package compilerxyz.main.gen;

import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 1006L, identityId = 4006L)
@BuiltinPath("eval.aspect_bool_const")
public interface AspectBoolConst extends Aspect {
    boolean aspectBoolConstGet(Value value);
}
