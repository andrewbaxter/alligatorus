package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.binding.BindingValue;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.gen.AspectBind;

public final class AspectBindObjectConst implements AspectBind {
    public static final AspectBindObjectConst INSTANCE = new AspectBindObjectConst();

    private AspectBindObjectConst() {}

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsSome()) {
            return EResult.ok(
                    new BindResult(Code.none(loc), new BindingValue(cv.value().optUnwrap())));
        }
        return DynUtil.bindViaTarget(xx, loc, cv);
    }
}
