package compilerxyz.main.gen;

import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1066L, identityId = 4066L)
public interface AspectNumericDynamic extends Aspect {
    NumericKind aspectNumericDynamicKind();
}
