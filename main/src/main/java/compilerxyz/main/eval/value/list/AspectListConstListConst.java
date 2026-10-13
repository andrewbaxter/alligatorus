package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.ConstHost;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectListConst;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.TList;

public record AspectListConstListConst<T extends Type>(TypeListConst<T> type) implements AspectListConst {

    @Override
    public Type aspectListConstElementType(Value value) {
        return type.elementType;
    }

    @Override
    public EResult<TList<Value>> aspectListConstElements(ExprContext xx, QExprId loc, Value value) {
        var hosts = TypeListConst.hostElements(
                MustCast.mustCast(ValueObjectConst.class, value).data());
        var out = new TList<Value>(hosts.size());
        for (var host : hosts) {
            var liftRes = ConstHost.lift(xx, loc, type.elementType, host);
            if (liftRes.hasErrors()) return liftRes.reerr();
            out.add(liftRes.requireValue("TypeListConst::elements lift"));
        }
        return EResult.ok(out);
    }
}
