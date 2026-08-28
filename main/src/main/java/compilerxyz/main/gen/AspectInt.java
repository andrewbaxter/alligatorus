package compilerxyz.main.gen;

import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 1008L, identityId = 4008L)
@BuiltinPath("eval.aspect_int")
public interface AspectInt extends Aspect {
    long aspectIntGet(Value value);
}
