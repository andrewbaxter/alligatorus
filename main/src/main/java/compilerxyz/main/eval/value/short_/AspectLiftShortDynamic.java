package compilerxyz.main.eval.value.short_;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;

public final class AspectLiftShortDynamic implements AspectLift {
    public static final AspectLiftShortDynamic INSTANCE = new AspectLiftShortDynamic();

    private AspectLiftShortDynamic() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        var d = MustCast.mustCast(Short.class, externalHostValue.optUnwrap());
        return EResult.ok(new ValueObjectConst(new TypeShortConst(), d));
    }
}
