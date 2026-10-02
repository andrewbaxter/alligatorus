package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.TList;

public record AspectLiftListBoxedDynamic<T extends Type>(TypeListBoxedDynamic<T> type, AspectLift elementLift)
        implements AspectLift {

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        var host = MustCast.mustCast(TList.class, externalHostValue);
        var elements = new TList<Value>(host.size());
        for (var element : host) {
            var liftRes = elementLift.aspectLiftLift(xx, loc, element);
            if (liftRes.hasErrors()) return liftRes.reerr();
            elements.add(liftRes.requireValue("TypeListBoxedDynamic::lift element"));
        }
        return EResult.ok(new ValueListConst<>(new TypeListConst<>(type.elementType), elements));
    }
}
