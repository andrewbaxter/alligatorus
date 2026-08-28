package compilerxyz.main.gen;

import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1033L, identityId = 4033L)
public interface AspectDynamicBoxed extends Aspect {
    Type aspectDynamicBoxedBoxedForm();
}
