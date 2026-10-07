package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.gen.AspectBind;

public final class AspectBindObjectConst implements AspectBind {
    public static final AspectBindObjectConst INSTANCE = new AspectBindObjectConst();

    private AspectBindObjectConst() {}

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindViaTarget(xx, loc, cv);
    }
}
