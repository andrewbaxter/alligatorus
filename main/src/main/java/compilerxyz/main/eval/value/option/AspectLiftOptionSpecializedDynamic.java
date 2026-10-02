package compilerxyz.main.eval.value.option;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;

public record AspectLiftOptionSpecializedDynamic<T extends Type>(
        TypeOptionSpecializedDynamic<T> type, AspectLift elementLift) implements AspectLift {

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        return TypeOptionConst.lift(
                xx,
                loc,
                type.elementType,
                elementLift,
                MustCast.mustCast(OptionStore.class, externalHostValue).optionStoreBoxed());
    }
}
