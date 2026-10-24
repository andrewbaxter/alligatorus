package compilerxyz.main.eval.value.list;

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
import compilerxyz.main.gen.AspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFieldListDynamic<T extends Type>(TypeListDynamic<T> type) implements AspectField {
    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeListDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        if (ck.equals(TypeListDynamic.LEN_KEY)) {
            var selfRes = DynUtil.valueReg(xx, loc, self);
            if (selfRes.hasErrors()) return selfRes.reerr();
            var b = new JavaBytecodeBuilder(loc);
            var out = b.invokeVirtual(
                    type.storeName, "size", TypeListDynamic.SIZE_DESCRIPTOR, b.reg(selfRes.requireValue("selfDyn")));
            return EResult.ok(DynUtil.regToValue(b.code(), out, TypeListDynamic.lenType()));
        }
        var methodOpt = type.methodTypes(xx).getOpt(ck);
        if (methodOpt.optIsNone()) {
            return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on list");
        }
        return EResult.ok(DynUtil.methodRefOn(self, methodOpt.optUnwrap()));
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        var keys = TList.from(type.methodTypes(xx).keySet());
        keys.add(TypeListDynamic.LEN_KEY);
        return keys;
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        if (key.equals(TypeListDynamic.LEN_KEY)) return Opt.some(TypeListDynamic.lenType());
        return type.methodTypes(xx).getOpt(key);
    }
}
