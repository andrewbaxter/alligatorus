package compilerxyz.main.eval.value.function;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.empty.ValueEmpty;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;

public final class AspectLiftFunctionType implements AspectLift {
    public static final AspectLiftFunctionType INSTANCE = new AspectLiftFunctionType();

    private AspectLiftFunctionType() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        return EResult.ok(new ValueEmpty(MustCast.mustCast(Type.class, externalHostValue)));
    }
}
