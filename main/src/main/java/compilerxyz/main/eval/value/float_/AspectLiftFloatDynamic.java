package compilerxyz.main.eval.value.float_;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;

public final class AspectLiftFloatDynamic implements AspectLift {
    public static final AspectLiftFloatDynamic INSTANCE = new AspectLiftFloatDynamic();

    private AspectLiftFloatDynamic() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        var d = MustCast.mustCast(Float.class, externalHostValue.optUnwrap());
        return EResult.ok(new ValueObjectConst(new TypeFloatConst(), d));
    }
}
