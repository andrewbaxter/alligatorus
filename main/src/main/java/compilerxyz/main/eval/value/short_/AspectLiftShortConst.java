package compilerxyz.main.eval.value.short_;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;

public final class AspectLiftShortConst implements AspectLift {
    public static final AspectLiftShortConst INSTANCE = new AspectLiftShortConst();

    private AspectLiftShortConst() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        var d = MustCast.mustCast(Short.class, externalHostValue);
        return EResult.ok(new ValueObjectConst(new TypeShortConst(), d));
    }
}
