package compilerxyz.main.gen;

import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 1010L, identityId = 4010L)
@BuiltinPath("eval.aspect_type")
public interface AspectType extends Aspect {
    Type aspectTypeType(Value value);
}
