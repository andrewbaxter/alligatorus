package compilerxyz.main.eval.aspect;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.gen.AspectBind;

public final class AspectBindViaTarget implements AspectBind {
    public static final AspectBindViaTarget INSTANCE = new AspectBindViaTarget();

    private AspectBindViaTarget() {}

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindViaTarget(xx, loc, cv);
    }
}
