package compilerxyz.main.eval.value.code;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.gen.AspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class AspectFieldCodeTargetDynamic implements AspectField {
    public static final AspectFieldCodeTargetDynamic INSTANCE = new AspectFieldCodeTargetDynamic();

    private AspectFieldCodeTargetDynamic() {}

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "code.target: field key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        if (!ck.equals(TypeCodeTargetDynamic.CAST_KEY)) {
            return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on code.target");
        }
        if (self.value().optIsNone() || !(self.value().optUnwrap() instanceof ValueDynamic selfVd)) {
            return EResult.err(loc, "code.target: receiver has no dynamic source");
        }
        return EResult.ok(new CodeValue(
                self.code(),
                EvalOutcome.ofValue(new ValueDynamic(selfVd.source(), new TypeCodeTargetCastFnDynamic()))));
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return TList.of(TypeCodeTargetDynamic.CAST_KEY);
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        if (key.equals(TypeCodeTargetDynamic.CAST_KEY)) return Opt.some(new TypeCodeTargetCastFnDynamic());
        return Opt.none();
    }
}
