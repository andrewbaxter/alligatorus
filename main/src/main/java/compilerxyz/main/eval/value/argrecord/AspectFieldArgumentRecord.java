package compilerxyz.main.eval.value.argrecord;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectField;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class AspectFieldArgumentRecord implements AspectField {
    public static final AspectFieldArgumentRecord INSTANCE = new AspectFieldArgumentRecord();

    private AspectFieldArgumentRecord() {}

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "ArgumentRecord::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        var record = MustCast.mustCast(ArgumentRecord.class, self.value().optUnwrap());
        var entryOpt = record.fields().getOpt(ck);
        if (entryOpt.optIsNone()) {
            return EResult.err(loc, "Field '" + ck.constKeyToCanonical() + "' not found on argument record");
        }
        var entry = entryOpt.optUnwrap();
        return EResult.ok(entry.binding()
                .bindingUse(
                        xx, loc, new BindingSetterArgumentField(record.fields(), ck, record.setter()), Code.none(loc)));
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return new TList<>();
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return Opt.none();
    }
}
