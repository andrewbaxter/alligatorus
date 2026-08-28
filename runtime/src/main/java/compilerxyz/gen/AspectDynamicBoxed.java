package compilerxyz.gen;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;

@BuiltinFull(uniqueId = 1033L, identityId = 4033L)
public interface AspectDynamicBoxed extends Aspect {
    Type aspectDynamicBoxedBoxedForm();
}
