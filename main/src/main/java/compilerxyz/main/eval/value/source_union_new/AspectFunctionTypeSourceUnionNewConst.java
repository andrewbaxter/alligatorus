package compilerxyz.main.eval.value.source_union_new;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.prelude.util.Opt;

public record AspectFunctionTypeSourceUnionNewConst(TypeTypeSourceUnionNewConst type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue function, CodeValue argument) {
        return GenAspectFunction.getMaybe(xx, type.asCtor())
                .optUnwrap()
                .aspectFunctionCall(xx, loc, function, argument);
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
