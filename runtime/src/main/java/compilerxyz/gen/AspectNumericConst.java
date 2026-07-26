package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.NumericKind;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;

@Builtin(uniqueId = 1065L, identityId = 4065L)
public interface AspectNumericConst extends Aspect {
  NumericKind aspectNumericConstKind();

  Number aspectNumericConstGet(Value value);
}
