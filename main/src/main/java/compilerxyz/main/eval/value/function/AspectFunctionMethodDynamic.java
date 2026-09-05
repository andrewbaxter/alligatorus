package compilerxyz.main.eval.value.function;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.prelude.util.Opt;

public record AspectFunctionMethodDynamic(TypeMethodDynamic type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        return TypeMethodShared.callDynamic(
                xx,
                loc,
                self,
                argument,
                "TypeMethodDynamic",
                type.arg(),
                type.ret(),
                type.klass(),
                type.methodName(),
                type.descriptor(),
                type.isInterface(),
                type.needsEvalContext());
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
