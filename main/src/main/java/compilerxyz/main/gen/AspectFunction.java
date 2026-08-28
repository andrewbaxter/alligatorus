package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 1116L, identityId = 4116L)
@BuiltinPath("eval.aspect_function")
public interface AspectFunction extends Aspect {
    EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self_, CodeValue argument);

    Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self);
}
