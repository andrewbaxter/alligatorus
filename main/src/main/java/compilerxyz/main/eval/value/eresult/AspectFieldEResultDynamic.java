package compilerxyz.main.eval.value.eresult;

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
import compilerxyz.main.eval.value.bool.TypeBoolDynamic;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFieldEResultDynamic<T extends Type>(TypeEResultDynamic<T> type) implements AspectField {

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeEResultDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        var ckStr =
                switch (ck) {
                    case ConstKey.Str s -> Opt.some(s);
                    case ConstKey.Uint u -> Opt.<ConstKey.Str>none();
                };
        if (ckStr.optIsSome() && ckStr.optUnwrap().value().equals("has_errors")) {
            var selfRes = DynUtil.valueReg(loc, self);
            if (selfRes.hasErrors()) return selfRes.reerr();
            var b = new JavaBytecodeBuilder(loc);
            var out =
                    b.invokeVirtual(GenCodegenDescriptors.E_RESULT_HAS_ERRORS, b.reg(selfRes.requireValue("selfDyn")));
            return EResult.ok(DynUtil.regToValue(b.code(), out, new TypeBoolDynamic()));
        }
        if (ckStr.optIsSome() && ckStr.optUnwrap().value().equals("value")) {
            var selfRes = DynUtil.valueReg(loc, self);
            if (selfRes.hasErrors()) return selfRes.reerr();
            var b = new JavaBytecodeBuilder(loc);
            var selfReg = b.reg(selfRes.requireValue("selfDyn"));
            var what = b.constString("eresult.value");
            var value = b.invokeVirtual(GenCodegenDescriptors.E_RESULT_REQUIRE_VALUE, selfReg, what);
            var elemConsumeRes = GenAspectConsumeDynamic.get(xx, loc, type.elementType());
            if (elemConsumeRes.hasErrors()) return elemConsumeRes.reerr();
            var elemDesc = elemConsumeRes
                    .requireValue("TypeEResultDynamic::field value AspectConsumeDynamic")
                    .aspectConsumeDynamicJvmClass(xx);
            if (elemDesc.value().startsWith("L") && !elemDesc.value().equals("Ljava/lang/Object;")) {
                value = b.checkcast(elemDesc, value);
            }
            return EResult.ok(DynUtil.regToValue(b.code(), value, type.elementType()));
        }
        return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on eresult");
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return TList.of(new ConstKey.Str("has_errors"), new ConstKey.Str("value"));
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        if (key.equals(new ConstKey.Str("has_errors"))) return Opt.some(new TypeBoolDynamic());
        if (key.equals(new ConstKey.Str("value"))) return Opt.some(type.elementType());
        return Opt.none();
    }
}
