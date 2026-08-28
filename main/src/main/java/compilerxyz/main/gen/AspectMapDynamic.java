package compilerxyz.main.gen;

import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1012L, identityId = 4012L)
public interface AspectMapDynamic extends Aspect {
    Type aspectMapDynamicKeyType();

    Type aspectMapDynamicValueType();
}
