package compilerxyz.gen;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.eval.aspect.Aspect;

@BuiltinFull(uniqueId = 1008L, identityId = 4008L)
@BuiltinPath("eval.aspect_int")
public interface AspectInt extends Aspect {
    long aspectIntGet(Value value);
}
