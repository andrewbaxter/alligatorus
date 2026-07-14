package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;

@BuiltinAspect(
    uniqueId = 1027L,
    kind = "Iterable",
    javadoc = "Allows foreach expressions to be run on the value.")
public interface AspectIterable extends Aspect {
  Type aspectIterableElementType();

  EResult<CodeValue> aspectIterableForeach(
      ExprContext xx, QExprId loc, CodeValue base, BindingId elementKey, CodeValue body);
}
