package compilerxyz.main.gen;

import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1010L, identityId = 4010L)
public interface AspectType extends Aspect {
    Type aspectTypeType(Value value);
}
