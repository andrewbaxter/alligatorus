package compilerxyz.main.eval.aspect;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.gen.AspectDropDynamicLight;

public final class AspectDropDynamicLightDefault implements AspectDropDynamicLight {
    public static final AspectDropDynamicLightDefault INSTANCE = new AspectDropDynamicLightDefault();

    private AspectDropDynamicLightDefault() {}

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(loc, cv);
    }
}
