package compilerxyz.main.eval.value.ushort;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;

public final class AspectLiftUshortDynamic implements AspectLift {
    public static final AspectLiftUshortDynamic INSTANCE = new AspectLiftUshortDynamic();

    private AspectLiftUshortDynamic() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        var d = MustCast.mustCast(Short.class, externalHostValue.optUnwrap());
        return EResult.ok(new ValueObjectConst(new TypeUshortConst(), d));
    }
}
