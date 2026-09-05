package compilerxyz.main.eval.value.argrecord;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.binding.BindingSetterConsumed;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.GenAspectNumericConst;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class AspectFieldArgumentTuple implements AspectField {
    public static final AspectFieldArgumentTuple INSTANCE = new AspectFieldArgumentTuple();

    private AspectFieldArgumentTuple() {}

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        if (key.value().optIsNone()) {
            return EResult.err(loc, "ArgumentTuple::field: key has no compile-time value");
        }
        var keyVal = key.value().optUnwrap();
        var numConst = GenAspectNumericConst.getMaybe(xx, keyVal.valueType());
        if (numConst.optIsNone()) {
            return EResult.err(loc, "ArgumentTuple::field: key must be a compile-time number");
        }
        long raw = numConst.optUnwrap().aspectNumericConstGet(keyVal).longValue();
        int idx = (int) raw;
        if ((long) idx != raw) {
            return EResult.err(loc, "ArgumentTuple::field: index out of int range, got " + raw);
        }
        var tuple = MustCast.mustCast(ArgumentTuple.class, self.value().optUnwrap());
        if (idx < 0 || idx >= tuple.elements().size()) {
            return EResult.err(loc, "ArgumentTuple::field: index " + idx + " out of range");
        }
        return EResult.ok(tuple.elements().get(idx).bindingUse(xx, loc, new BindingSetterConsumed(), Code.none(loc)));
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        var out = new TList<ConstKey>();
        return out;
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return Opt.none();
    }
}
