package compilerxyz.main.eval.value.string;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;

public final class AspectLiftStringDynamic implements AspectLift {
    public static final AspectLiftStringDynamic INSTANCE = new AspectLiftStringDynamic();

    private AspectLiftStringDynamic() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        var s = MustCast.mustCast(String.class, externalHostValue.optUnwrap());
        return EResult.ok(new ValueObjectConst(new TypeStringConst(), s));
    }
}
