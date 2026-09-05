package compilerxyz.main.eval.value.source;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.prelude.util.Opt;

public final class AspectFunctionSourceVirtual implements AspectFunction {
    public static final AspectFunctionSourceVirtual INSTANCE = new AspectFunctionSourceVirtual();

    private AspectFunctionSourceVirtual() {}

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var argRes = SourceSpecs.argValue(loc, argument, "source.virtual");
        if (argRes.hasErrors()) return argRes.reerr();
        var nameRes = SourceSpecs.constString(xx, loc, argRes.requireValue("source.virtual arg"), "source.virtual");
        if (nameRes.hasErrors()) return nameRes.reerr();
        var concatRes = Eval.codeConcat(xx, loc, self.code(), argument.code());
        if (concatRes.hasErrors()) return concatRes.reerr();
        return EResult.ok(new CodeValue(
                concatRes.requireValue("concat"),
                EvalOutcome.ofValue(
                        SourceSpecs.spec(new SourceSpecValue.Virtual(nameRes.requireValue("source.virtual name"))))));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
