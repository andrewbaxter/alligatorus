package compilerxyz.main.eval.value.source;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.binding.BindingValue;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Opt;

public final class AspectBindBundle implements AspectBind {
    public static final AspectBindBundle INSTANCE = new AspectBindBundle();

    private AspectBindBundle() {}

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return switch (cv.value()) {
            case Opt.Some<Value> some -> EResult.ok(new BindResult(cv.code(), new BindingValue(some.value())));
            case Opt.None<Value> none -> EResult.err(loc, "TypeBundle::bind: CodeValue has no compile-time value");
        };
    }
}
