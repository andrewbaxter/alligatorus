package compilerxyz.main.eval.value.option;

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
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.bool.TypeBoolDynamic;
import compilerxyz.main.gen.AspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFieldOptionSpecializedDynamic<T extends Type>(TypeOptionSpecializedDynamic<T> type)
        implements AspectField {
    private static final ConstKey VALUE = new ConstKey.Str("value");
    private static final ConstKey IS_SOME = new ConstKey.Str("is_some");

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeOptionSpecializedDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        if (ck.equals(VALUE) || ck.equals(IS_SOME)) {
            var selfRes = DynUtil.valueReg(loc, self);
            if (selfRes.hasErrors()) return selfRes.reerr();
            var b = new JavaBytecodeBuilder(loc);
            var selfReg = b.reg(selfRes.requireValue("selfDyn"));
            Type resultType;
            JavaBytecodeReg out;
            if (ck.equals(VALUE)) {
                out = b.invokeVirtual(
                        type.storeName,
                        "value",
                        JavaMethodDescriptor.fromParts(type.elementDescriptor(xx), TList.of()),
                        selfReg);
                resultType = type.elementType;
            } else {
                out = b.invokeVirtual(
                        type.storeName,
                        "isSome",
                        JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_BOOL, TList.of()),
                        selfReg);
                resultType = new TypeBoolDynamic();
            }
            return EResult.ok(DynUtil.regToValue(b.code(), out, resultType));
        }
        return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on option");
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return TList.of(VALUE, IS_SOME);
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        if (key.equals(IS_SOME)) return Opt.some(new TypeBoolDynamic());
        if (key.equals(VALUE)) return Opt.some(type.elementType);
        return Opt.none();
    }
}
