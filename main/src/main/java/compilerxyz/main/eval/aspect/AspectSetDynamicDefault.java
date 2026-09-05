package compilerxyz.main.eval.aspect;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.gen.AspectSet;

public final class AspectSetDynamicDefault implements AspectSet {
    public static final AspectSetDynamicDefault INSTANCE = new AspectSetDynamicDefault();

    private AspectSetDynamicDefault() {}

    @Override
    public EResult<CodeValue> aspectSetSet(ExprContext xx, QExprId loc, CodeValue base, CodeValue value) {
        return DynUtil.dynamicSet(xx, loc, base, value);
    }
}
