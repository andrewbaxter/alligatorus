package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;

public record AspectLiftEResultDynamic<T extends Type>(
        TypeEResultDynamic<T> type, AspectLift elementLift, AspectLift errLift) implements AspectLift {

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        var host = MustCast.mustCast(EResult.class, externalHostValue);
        Result<Value, TList<Value>> lifted;
        if (host.hasErrors()) {
            var errs = MustCast.mustCast(TList.class, host.errorsOpt().optUnwrap());
            var out = new TList<Value>(errs.size());
            for (var err : errs) {
                var liftRes = errLift.aspectLiftLift(xx, loc, err);
                if (liftRes.hasErrors()) return liftRes.reerr();
                out.add(liftRes.requireValue("TypeEResultDynamic::lift err"));
            }
            lifted = new Result.Err<>(out);
        } else {
            var liftRes = elementLift.aspectLiftLift(xx, loc, host.requireValue("TypeEResultDynamic::lift ok host"));
            if (liftRes.hasErrors()) return liftRes.reerr();
            lifted = new Result.Ok<>(liftRes.requireValue("TypeEResultDynamic::lift ok"));
        }
        return EResult.ok(new ValueEResultConst<>(new TypeEResultConst<>(type.elementType()), lifted));
    }
}
