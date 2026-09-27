package compilerxyz.main.eval.value.string;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;

public final class AspectLiftStringConst implements AspectLift {
    public static final AspectLiftStringConst INSTANCE = new AspectLiftStringConst();

    private AspectLiftStringConst() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        if (!(externalHostValue instanceof String str)) {
            return EResult.err(loc, "TypeStringConst::lift: expected host String");
        }
        return EResult.ok(new ValueObjectConst(new TypeStringConst(), str));
    }
}
