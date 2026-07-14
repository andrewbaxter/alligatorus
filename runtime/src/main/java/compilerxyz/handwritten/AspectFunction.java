package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.util.Opt;

@BuiltinAspect(
    uniqueId = 1116L,
    kind = "Function",
    javadoc = "Allows the value of this type to be called (as a function)")
public interface AspectFunction extends Aspect {
  EResult<CodeValue> aspectFunctionCall(
      ExprContext xx, QExprId loc, CodeValue self_, CodeValue argument);

  Opt<Type> aspectFunctionExceptionType();
}
