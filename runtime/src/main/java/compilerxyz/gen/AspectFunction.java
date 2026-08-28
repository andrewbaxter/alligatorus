package compilerxyz.gen;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.util.Opt;

@BuiltinFull(uniqueId = 1116L, identityId = 4116L)
@BuiltinPath("eval.aspect_function")
public interface AspectFunction extends Aspect {
    EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self_, CodeValue argument);

    Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self);
}
