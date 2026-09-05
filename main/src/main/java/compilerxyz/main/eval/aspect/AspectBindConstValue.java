package compilerxyz.main.eval.aspect;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.binding.BindingValue;
import compilerxyz.main.gen.AspectBind;

public final class AspectBindConstValue implements AspectBind {
    public static final AspectBindConstValue INSTANCE = new AspectBindConstValue();

    private AspectBindConstValue() {}

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsSome()) {
            return EResult.ok(
                    new BindResult(cv.code(), new BindingValue(cv.value().optUnwrap())));
        }
        return EResult.err(loc, "AspectBindConstValue::bind: CodeValue has no compile-time value");
    }
}
