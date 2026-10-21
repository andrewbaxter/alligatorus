package compilerxyz.main.eval.value.array;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.value.array.TypeArrayDynamic.FieldSpec;
import compilerxyz.main.gen.AspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFieldArrayDynamic<T extends Type>(TypeArrayDynamic<T> type) implements AspectField {

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeArrayDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        var specOpt = type.fieldSpecs().getOpt(ck);
        if (specOpt.optIsNone()) {
            return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on array");
        }
        return switch (specOpt.optUnwrap()) {
            case FieldSpec.Length l -> {
                var selfRes = DynUtil.valueReg(xx, loc, self);
                if (selfRes.hasErrors()) yield selfRes.reerr();
                var b = new JavaBytecodeBuilder(loc);
                var out = type.length(b, b.reg(selfRes.requireValue("selfDyn")));
                yield EResult.ok(DynUtil.regToValue(b.code(), out, l.fieldSpecFieldType()));
            }
            case FieldSpec.Method m -> EResult.ok(DynUtil.methodRefOn(self, m.type()));
        };
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return type.fieldSpecs().keys();
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return type.fieldSpecs().getOpt(key).optMap(spec -> spec.fieldSpecFieldType());
    }
}
