package compilerxyz.main.eval.value.bool;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Opt;

public final class AspectLiftBoolDynamic implements AspectLift {
    public static final AspectLiftBoolDynamic INSTANCE = new AspectLiftBoolDynamic();

    private AspectLiftBoolDynamic() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        if (externalHostValue.optIsNone()) {
            return EResult.err(loc, "TypeBoolDynamic::lift: expected java.lang.Boolean, got null");
        }
        var rawHostValue = externalHostValue.optUnwrap();
        if (!(rawHostValue instanceof Boolean b)) {
            return EResult.err(
                    loc,
                    "TypeBoolDynamic::lift: expected java.lang.Boolean, got "
                            + rawHostValue.getClass().getName());
        }
        return EResult.ok(new ValueObjectConst(new TypeBoolConst(), b));
    }
}
