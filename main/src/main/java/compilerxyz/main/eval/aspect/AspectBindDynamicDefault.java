package compilerxyz.main.eval.aspect;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.BindSubDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.prelude.util.Opt;

public final class AspectBindDynamicDefault implements AspectBindDynamic {
    public static final AspectBindDynamicDefault INSTANCE = new AspectBindDynamicDefault();

    private AspectBindDynamicDefault() {}

    @Override
    public EResult<Opt<BindSubDynamic>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindDynamicDefault(xx, loc, cv);
    }
}
