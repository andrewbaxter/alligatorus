package compilerxyz.main.eval.value.record;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFieldRecord(Record type) implements AspectField {

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "Record::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        return type.accessOrDrop(xx, loc, self, Opt.some(ckOpt.optUnwrap()));
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        if (type.base().optIsNone()) {
            return new TList<>();
        }
        var out = new TList<ConstKey>(type.fields().size());
        for (var k : type.fields().keys()) out.add(k);
        return out;
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        var fOpt = type.fields().getOpt(key);
        if (fOpt.optIsNone()) return Opt.none();
        var f = fOpt.optUnwrap();
        return Opt.some(Record.recordFieldType(f));
    }
}
