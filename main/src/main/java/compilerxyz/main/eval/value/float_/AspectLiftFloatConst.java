package compilerxyz.main.eval.value.float_;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;

public final class AspectLiftFloatConst implements AspectLift {
    public static final AspectLiftFloatConst INSTANCE = new AspectLiftFloatConst();

    private AspectLiftFloatConst() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        var d = MustCast.mustCast(Float.class, externalHostValue);
        return EResult.ok(new ValueObjectConst(new TypeFloatConst(), d));
    }
}
