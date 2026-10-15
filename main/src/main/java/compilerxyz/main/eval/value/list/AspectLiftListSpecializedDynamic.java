package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.TypeForms;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;

public record AspectLiftListSpecializedDynamic<T extends Type>(TypeListSpecializedDynamic<T> type)
        implements AspectLift {

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        var host = MustCast.mustCast(ListStore.class, externalHostValue);
        return EResult.ok(new ValueObjectConst(new TypeListConst<>(TypeForms.constForm(xx, type.elementType)), host));
    }
}
