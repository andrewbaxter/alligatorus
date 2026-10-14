package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;

public record AspectFunctionSourceObjectDynamic(TypeSourceObjectDynamic type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        if (self.value().optIsNone() || !(self.value().optUnwrap() instanceof ValueObjectConst instance)) {
            return EResult.err(loc, type.typeDesc() + "::call: receiver is not a constant instance");
        }
        return MustCast.mustCast(AspectFunction.class, instance.data()).aspectFunctionCall(xx, loc, self, argument);
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        if (self.value().optIsNone() || !(self.value().optUnwrap() instanceof ValueObjectConst instance)) {
            return Opt.none();
        }
        return MustCast.mustCast(AspectFunction.class, instance.data()).aspectFunctionExceptionType(xx, loc, self);
    }
}
