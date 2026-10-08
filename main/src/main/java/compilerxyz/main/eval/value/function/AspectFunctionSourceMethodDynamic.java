package compilerxyz.main.eval.value.function;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.prelude.util.Opt;

public record AspectFunctionSourceMethodDynamic(TypeSourceMethodDynamic type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var callRes = TypeMethodShared.callDynamic(
                xx,
                loc,
                self,
                argument,
                "TypeSourceMethodDynamic",
                type.arg(),
                type.ret(),
                type.klass(),
                type.methodName(),
                type.descriptor(),
                type.isInterface(),
                type.needsEvalContext());
        if (callRes.hasErrors() || type.set().optIsNone()) return callRes;
        var call = callRes.requireValue("TypeSourceMethodDynamic call");
        var b = new JavaBytecodeBuilder(loc);
        b.defSet(type.set().optUnwrap());
        var carryRes = b.addCode(loc, call.code());
        if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
        return EResult.ok(new CodeValue(Code.target(b.code()), call.outcome()));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
