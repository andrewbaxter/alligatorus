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
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.int_.TypeIntDynamic;
import compilerxyz.main.gen.AspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFieldListSpecializedDynamic<T extends Type>(TypeListSpecializedDynamic<T> type)
        implements AspectField {

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeListSpecializedDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        if (ck.equals(new ConstKey.Str("push"))) {
            return EResult.ok(DynUtil.methodRefOn(self, new TypeListSpecializedPushDynamic(type)));
        }
        if (ck.equals(new ConstKey.Str("get"))) {
            return EResult.ok(DynUtil.methodRefOn(self, new TypeListGetDynamic(type)));
        }
        if (ck.equals(new ConstKey.Str("set"))) {
            return EResult.ok(DynUtil.methodRefOn(self, new TypeListSetDynamic(type)));
        }
        if (ck.equals(new ConstKey.Str("len"))) {
            var selfRes = DynUtil.valueReg(loc, self);
            if (selfRes.hasErrors()) return selfRes.reerr();
            var b = new JavaBytecodeBuilder(loc);
            var sizeDesc = JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_INT, TList.of());
            var out = b.invokeVirtual(type.storeName, "size", sizeDesc, b.reg(selfRes.requireValue("selfDyn")));
            return EResult.ok(DynUtil.regToValue(b.code(), out, new TypeIntDynamic()));
        }
        return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on list");
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return TList.of(
                new ConstKey.Str("push"), new ConstKey.Str("get"), new ConstKey.Str("set"), new ConstKey.Str("len"));
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        if (key.equals(new ConstKey.Str("push"))) {
            return Opt.some(new TypeListSpecializedPushDynamic(type));
        }
        if (key.equals(new ConstKey.Str("get"))) {
            return Opt.some(new TypeListGetDynamic(type));
        }
        if (key.equals(new ConstKey.Str("set"))) {
            return Opt.some(new TypeListSetDynamic(type));
        }
        if (key.equals(new ConstKey.Str("len"))) {
            return Opt.some(new TypeIntDynamic());
        }
        return Opt.none();
    }
}
