package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.prelude.util.Opt;

public record AspectFunctionObjectConst(TypeObjectConst type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var innerRes = type.extractInnerType(loc, self, "call");
        if (innerRes.hasErrors()) return innerRes.reerr();
        var inner = innerRes.requireValue(type.typeDesc() + "::call inner");
        var asp = GenAspectFunction.get(xx, loc, inner);
        if (asp.hasErrors()) return asp.reerr();
        return asp.requireValue(type.typeDesc() + "::call inner AspectFunction")
                .aspectFunctionCall(xx, loc, self, argument);
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        var innerRes = type.extractInnerType(loc, self, "exception_type");
        if (innerRes.hasErrors()) return Opt.none();
        var innerAsp =
                GenAspectFunction.getMaybe(xx, innerRes.requireValue(type.typeDesc() + "::exception_type inner"));
        if (innerAsp.optIsNone()) return Opt.none();
        return innerAsp.optUnwrap().aspectFunctionExceptionType(xx, loc, self);
    }
}
