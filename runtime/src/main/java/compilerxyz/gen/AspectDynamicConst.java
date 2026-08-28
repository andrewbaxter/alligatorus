package compilerxyz.gen;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;

@BuiltinFull(uniqueId = 1048L, identityId = 4046L)
public interface AspectDynamicConst extends Aspect {
    Type aspectDynamicConstConstForm();
}
