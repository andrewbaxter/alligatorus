package compilerxyz.main.eval.value.option;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;

public record AspectLiftOptionDynamic<T extends Type>(TypeOptionDynamic<T> type, AspectLift elementLift)
        implements AspectLift {

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        return TypeOptionConst.lift(
                xx, loc, type.elementType, elementLift, MustCast.mustCast(Opt.class, externalHostValue));
    }
}
