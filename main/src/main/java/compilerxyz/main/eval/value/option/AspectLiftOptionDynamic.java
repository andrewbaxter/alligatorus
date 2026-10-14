package compilerxyz.main.eval.value.option;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.TypeForms;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;

public record AspectLiftOptionDynamic<T extends Type>(TypeOptionDynamic<T> type) implements AspectLift {

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        var host = MustCast.mustCast(Opt.class, externalHostValue);
        return EResult.ok(new ValueObjectConst(new TypeOptionConst<>(TypeForms.constForm(xx, type.elementType)), host));
    }
}
