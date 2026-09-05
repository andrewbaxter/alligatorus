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

public final class AspectFunctionSourceLocal implements AspectFunction {
    public static final AspectFunctionSourceLocal INSTANCE = new AspectFunctionSourceLocal();

    private AspectFunctionSourceLocal() {}

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var argRes = SourceSpecs.argValue(loc, argument, "source.local");
        if (argRes.hasErrors()) return argRes.reerr();
        var pathRes = SourceSpecs.constString(xx, loc, argRes.requireValue("source.local arg"), "source.local");
        if (pathRes.hasErrors()) return pathRes.reerr();
        var concatRes = Eval.codeConcat(xx, loc, self.code(), argument.code());
        if (concatRes.hasErrors()) return concatRes.reerr();
        return EResult.ok(new CodeValue(
                concatRes.requireValue("concat"),
                EvalOutcome.ofValue(
                        SourceSpecs.spec(new SourceSpecValue.Local(pathRes.requireValue("source.local path"))))));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
