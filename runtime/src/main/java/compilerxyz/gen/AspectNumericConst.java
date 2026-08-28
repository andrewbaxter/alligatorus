package compilerxyz.gen;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.NumericKind;
import compilerxyz.eval.aspect.Aspect;

@BuiltinFull(uniqueId = 1065L, identityId = 4065L)
public interface AspectNumericConst extends Aspect {
    NumericKind aspectNumericConstKind();

    Number aspectNumericConstGet(Value value);
}
