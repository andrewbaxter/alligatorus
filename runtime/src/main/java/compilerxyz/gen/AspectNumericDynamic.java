package compilerxyz.gen;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.NumericKind;
import compilerxyz.eval.aspect.Aspect;

@BuiltinFull(uniqueId = 1066L, identityId = 4066L)
public interface AspectNumericDynamic extends Aspect {
    NumericKind aspectNumericDynamicKind();
}
