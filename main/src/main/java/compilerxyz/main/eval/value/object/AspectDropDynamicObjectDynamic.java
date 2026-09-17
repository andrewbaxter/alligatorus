package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectDropDynamic;

public final class AspectDropDynamicObjectDynamic implements AspectDropDynamic {
    public static final AspectDropDynamicObjectDynamic INSTANCE = new AspectDropDynamicObjectDynamic();

    private AspectDropDynamicObjectDynamic() {}

    @Override
    public EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv) {
        return EResult.ok(Code.none(loc));
    }
}
