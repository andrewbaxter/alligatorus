package compilerxyz.main.eval.value.ushort;

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

public final class AspectFieldUshortDynamic implements AspectField {
    public static final AspectFieldUshortDynamic INSTANCE = new AspectFieldUshortDynamic();

    private AspectFieldUshortDynamic() {}

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeUshortDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        var fieldTypeOpt = aspectFieldTypeOpt(xx, ck);
        if (fieldTypeOpt.optIsNone()) {
            return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on dynamic ushort");
        }
        return EResult.ok(
                new CodeValue(self.code(), EvalOutcome.ofValue(ValueDynamic.stack(xx, fieldTypeOpt.optUnwrap()))));
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return TypeUshortDynamic.METHOD_FIELDS.keys();
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return TypeUshortDynamic.METHOD_FIELDS.getOpt(key).optMap(t -> t);
    }
}
