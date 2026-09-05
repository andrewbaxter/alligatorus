package compilerxyz.main.eval.value.ulong;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;

public final class AspectLiftUlongDynamic implements AspectLift {
    public static final AspectLiftUlongDynamic INSTANCE = new AspectLiftUlongDynamic();

    private AspectLiftUlongDynamic() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        var d = MustCast.mustCast(Long.class, externalHostValue.optUnwrap());
        return EResult.ok(new ValueObjectConst(new TypeUlongConst(), d));
    }
}
