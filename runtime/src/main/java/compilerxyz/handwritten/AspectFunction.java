package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.annotations.SourceExposed;
import compilerxyz.annotations.SourceInterface;
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
@SourceInterface(uniqueId = 1016L, scopeId = 3003)
@BuiltinPath("eval.aspect_function")
public interface AspectFunction extends Aspect {
  @SourceExposed(name = "call")
  EResult<CodeValue> aspectFunctionCall(
      ExprContext xx, QExprId loc, CodeValue self_, CodeValue argument);

  Opt<Type> aspectFunctionExceptionType();
}
