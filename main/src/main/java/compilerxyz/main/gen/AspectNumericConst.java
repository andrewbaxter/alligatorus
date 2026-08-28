package compilerxyz.main.gen;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1065L, identityId = 4065L)
public interface AspectNumericConst extends Aspect {
    NumericKind aspectNumericConstKind();

    Number aspectNumericConstGet(Value value);
}
