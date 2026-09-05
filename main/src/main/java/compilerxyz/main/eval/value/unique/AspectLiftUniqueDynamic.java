package compilerxyz.main.eval.value.unique;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;

public final class AspectLiftUniqueDynamic implements AspectLift {
    public static final AspectLiftUniqueDynamic INSTANCE = new AspectLiftUniqueDynamic();

    private AspectLiftUniqueDynamic() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        var u = MustCast.mustCast(Unique.class, externalHostValue.optUnwrap());
        return EResult.ok(new ValueObjectConst(new TypeUniqueConst(), u));
    }
}
