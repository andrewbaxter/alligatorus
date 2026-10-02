package compilerxyz.main.eval.value.byte_;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;

public final class AspectLiftByteConst implements AspectLift {
    public static final AspectLiftByteConst INSTANCE = new AspectLiftByteConst();

    private AspectLiftByteConst() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        var d = MustCast.mustCast(Byte.class, externalHostValue);
        return EResult.ok(new ValueObjectConst(new TypeByteConst(), d));
    }
}
