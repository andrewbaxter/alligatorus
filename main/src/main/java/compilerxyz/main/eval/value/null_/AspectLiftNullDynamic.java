package compilerxyz.main.eval.value.null_;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Opt;

public final class AspectLiftNullDynamic implements AspectLift {
    public static final AspectLiftNullDynamic INSTANCE = new AspectLiftNullDynamic();

    private AspectLiftNullDynamic() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        return EResult.ok(new ValueObjectConst(new TypeNullConst(), null));
    }
}
