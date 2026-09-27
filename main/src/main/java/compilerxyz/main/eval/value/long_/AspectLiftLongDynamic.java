package compilerxyz.main.eval.value.long_;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;

public final class AspectLiftLongDynamic implements AspectLift {
    public static final AspectLiftLongDynamic INSTANCE = new AspectLiftLongDynamic();

    private AspectLiftLongDynamic() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        var d = MustCast.mustCast(Long.class, externalHostValue);
        return EResult.ok(new ValueObjectConst(new TypeLongConst(), d));
    }
}
