package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;

public record AspectLiftEResultDynamic<T extends Type>(TypeEResultDynamic<T> type) implements AspectLift {

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        var host = MustCast.mustCast(EResult.class, externalHostValue);
        return EResult.ok(new ValueObjectConst(new TypeEResultConst<>(type.elementType()), host));
    }
}
