package compilerxyz.main.gen;

import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1048L, identityId = 4046L)
public interface AspectDynamicConst extends Aspect {
    Type aspectDynamicConstConstForm();
}
