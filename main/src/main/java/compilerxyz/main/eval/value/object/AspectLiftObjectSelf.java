package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;

public final class AspectLiftObjectSelf implements AspectLift {
    public static final AspectLiftObjectSelf INSTANCE = new AspectLiftObjectSelf();

    private AspectLiftObjectSelf() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        return EResult.ok(MustCast.mustCast(Value.class, externalHostValue));
    }
}
