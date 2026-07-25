package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.util.Opt;

@Builtin(uniqueId = 1116L)
@BuiltinPath("eval.aspect_function")
public interface AspectFunction extends Aspect {
  EResult<CodeValue> aspectFunctionCall(
      ExprContext xx, QExprId loc, CodeValue self_, CodeValue argument);

  Opt<Type> aspectFunctionExceptionType();
}
