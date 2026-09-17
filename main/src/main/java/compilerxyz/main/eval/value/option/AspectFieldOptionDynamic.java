package compilerxyz.main.eval.value.option;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.bool.TypeBoolDynamic;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFieldOptionDynamic<T extends Type>(TypeOptionDynamic<T> type) implements AspectField {

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeOptionDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        var ckStr =
                switch (ck) {
                    case ConstKey.Str s -> Opt.some(s);
                    case ConstKey.Uint u -> Opt.<ConstKey.Str>none();
                };
        if (ckStr.optIsSome() && ckStr.optUnwrap().value().equals("value")) {
            var selfRes = DynUtil.valueReg(loc, self);
            if (selfRes.hasErrors()) return selfRes.reerr();
            var b = new JavaBytecodeBuilder(loc);
            var some = b.checkcast(
                    JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(Opt.Some.class)),
                    b.reg(selfRes.requireValue("selfDyn")));
            var value = b.invokeVirtual(GenCodegenDescriptors.SOME_VALUE, some);
            var elemConsumeRes = GenAspectConsumeDynamic.get(xx, loc, type.elementType);
            if (elemConsumeRes.hasErrors()) return elemConsumeRes.reerr();
            var elemDesc = elemConsumeRes
                    .requireValue("TypeOptionDynamic::field value AspectConsumeDynamic")
                    .aspectConsumeDynamicJvmClass(xx);
            if (elemDesc.value().startsWith("L") && !elemDesc.value().equals("Ljava/lang/Object;")) {
                value = b.checkcast(elemDesc, value);
            }
            return EResult.ok(DynUtil.regToValue(b.code(), value, type.elementType));
        }
        if (ckStr.optIsSome() && ckStr.optUnwrap().value().equals("is_some")) {
            var selfRes = DynUtil.valueReg(loc, self);
            if (selfRes.hasErrors()) return selfRes.reerr();
            var b = new JavaBytecodeBuilder(loc);
            var out = b.instanceOf(JavaInternalName.fromClass(Opt.Some.class), b.reg(selfRes.requireValue("selfDyn")));
            return EResult.ok(DynUtil.regToValue(b.code(), out, new TypeBoolDynamic()));
        }
        var factory = OptionFieldSpecs.factories(type.elementType).getOrNull(ck);
        if (factory == null) {
            return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on option");
        }
        return EResult.ok(new CodeValue(
                Code.none(loc), EvalOutcome.ofValue(new ValueObjectConst(ValueObjectConst.plainTypeType(), factory))));
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return TList.from(OptionFieldSpecs.factories(type.elementType).keySet());
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        var keyStr =
                switch (key) {
                    case ConstKey.Str s -> Opt.some(s);
                    case ConstKey.Uint u -> Opt.<ConstKey.Str>none();
                };
        if (keyStr.optIsSome() && keyStr.optUnwrap().value().equals("is_some")) {
            return Opt.some(new TypeBoolDynamic());
        }
        if (keyStr.optIsSome() && keyStr.optUnwrap().value().equals("value")) {
            return Opt.some(type.elementType);
        }
        var factory = OptionFieldSpecs.factories(type.elementType).getOrNull(key);
        return factory == null ? Opt.none() : Opt.some(factory);
    }
}
