package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.NumericKind;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1066L, identityId = 4066L)
public interface AspectNumericDynamic extends Aspect {
  NumericKind aspectNumericDynamicKind();
}
