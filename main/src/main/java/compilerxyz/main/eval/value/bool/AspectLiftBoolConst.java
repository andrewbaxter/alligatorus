package compilerxyz.main.eval.value.bool;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;

public final class AspectLiftBoolConst implements AspectLift {
    public static final AspectLiftBoolConst INSTANCE = new AspectLiftBoolConst();

    private AspectLiftBoolConst() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        var d = MustCast.mustCast(Boolean.class, externalHostValue);
        return EResult.ok(new ValueObjectConst(new TypeBoolConst(), d));
    }
}
