package compilerxyz.main.gen;

import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 1047L, identityId = 4045L)
public interface AspectListConst extends Aspect {
    TList<Value> aspectListConstElements(Value value);

    Type aspectListConstElementType(Value value);
}
