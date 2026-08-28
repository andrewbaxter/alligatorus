package compilerxyz.main.gen;

import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1009L, identityId = 4009L)
public interface AspectStringConst extends Aspect {
    String aspectStringConstGet(Value value);
}
