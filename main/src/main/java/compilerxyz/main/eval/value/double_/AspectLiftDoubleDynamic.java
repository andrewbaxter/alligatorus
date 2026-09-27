package compilerxyz.main.eval.value.double_;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;

public final class AspectLiftDoubleDynamic implements AspectLift {
    public static final AspectLiftDoubleDynamic INSTANCE = new AspectLiftDoubleDynamic();

    private AspectLiftDoubleDynamic() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        var d = MustCast.mustCast(Double.class, externalHostValue);
        return EResult.ok(new ValueObjectConst(new TypeDoubleConst(), d));
    }
}
