package compilerxyz.main.gen;

import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 1009L, identityId = 4009L)
@BuiltinPath("eval.aspect_string_const")
public interface AspectStringConst extends Aspect {
    String aspectStringConstGet(Value value);
}
