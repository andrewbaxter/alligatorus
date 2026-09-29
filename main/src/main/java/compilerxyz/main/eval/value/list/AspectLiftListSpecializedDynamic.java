package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.TList;

public record AspectLiftListSpecializedDynamic<T extends Type>(
        TypeListSpecializedDynamic<T> type, AspectLift elementLift) implements AspectLift {

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        var host = MustCast.mustCast(ListStore.class, externalHostValue);
        var size = host.listStoreSize();
        var elements = new TList<Value>(size);
        for (int i = 0; i < size; i++) {
            var liftRes = elementLift.aspectLiftLift(xx, loc, host.listStoreBoxed(i));
            if (liftRes.hasErrors()) return liftRes.reerr();
            elements.add(liftRes.requireValue("TypeListSpecializedDynamic::lift element"));
        }
        return EResult.ok(new ValueObjectConst(new TypeListConst<>(type.elementType), elements));
    }
}
