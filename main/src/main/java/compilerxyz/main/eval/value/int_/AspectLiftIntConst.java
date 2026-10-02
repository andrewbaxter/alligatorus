package compilerxyz.main.eval.value.int_;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;

public final class AspectLiftIntConst implements AspectLift {
    public static final AspectLiftIntConst INSTANCE = new AspectLiftIntConst();

    private AspectLiftIntConst() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        var d = MustCast.mustCast(Integer.class, externalHostValue);
        return EResult.ok(new ValueObjectConst(new TypeIntConst(), d));
    }
}
