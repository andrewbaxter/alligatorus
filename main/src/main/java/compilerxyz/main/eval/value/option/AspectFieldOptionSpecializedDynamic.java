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
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.bool.TypeBoolDynamic;
import compilerxyz.main.eval.value.object.ValueObjectConst;
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
            var selfDynRes = DynUtil.dynamicConsumeToDynamic(loc, self);
            if (selfDynRes.hasErrors()) return selfDynRes.reerr();
            var seq = new JavaBytecodeSequence();
            seq.add(selfDynRes.requireValue("selfDyn"));
            Type resultType;
            if (ck.equals(VALUE)) {
                seq.add(JavaBytecodeUtils.callMethod(
                        loc,
                        type.storeName,
                        "value",
                        JavaMethodDescriptor.fromParts(type.elementDescriptor(xx), TList.of())));
                resultType = type.elementType;
            } else {
                seq.add(JavaBytecodeUtils.callMethod(
                        loc,
                        type.storeName,
                        "isSome",
                        JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_BOOL, TList.of())));
                resultType = new TypeBoolDynamic();
            }
            return EResult.ok(new CodeValue(
                    Code.target(new CodeDynamic(loc, seq)), EvalOutcome.ofValue(ValueDynamic.stack(xx, resultType))));
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
        if (key.equals(IS_SOME)) return Opt.some(new TypeBoolDynamic());
        if (key.equals(VALUE)) return Opt.some(type.elementType);
        var factory = OptionFieldSpecs.factories(type.elementType).getOrNull(key);
        return factory == null ? Opt.none() : Opt.some(factory);
    }
}
