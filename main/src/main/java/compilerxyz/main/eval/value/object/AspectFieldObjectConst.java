package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.value.function.TypeMethodDynamic;
import compilerxyz.main.eval.value.source_interface.TypeInterfaceMethodBuiltin;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.GenType;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;

public record AspectFieldObjectConst(TypeObjectConst type) implements AspectField {

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var innerRes = type.extractInnerType(loc, self, "field");
        if (innerRes.hasErrors()) return innerRes.reerr();
        var inner = innerRes.requireValue(type.typeDesc() + "::field inner");
        if (key.value().optIsSome()) {
            var ckRes = Eval.extractConstKey(xx, key.value().optUnwrap());
            switch (ckRes) {
                case Result.Ok<ConstKey, String> ok -> {
                    if (new AspectFieldObjectDynamic(GenType.Holder.INSTANCE)
                            .aspectFieldTypeOpt(xx, ok.value())
                            .optIsSome()) {
                        CodeValue selfLifted = self;
                        if (!DynUtil.dynamicHasCode(self)) {
                            var liftRes = new AspectConsumeDynamicObjectConst(type)
                                    .aspectConsumeDynamicToDynamic(xx, loc, self);
                            if (liftRes.hasErrors()) return liftRes.reerr();
                            selfLifted = liftRes.requireValue(type.typeDesc() + "::field lift");
                        }
                        return new AspectFieldObjectDynamic(GenType.Holder.INSTANCE)
                                .aspectFieldField(xx, loc, selfLifted, key);
                    }
                }
                case Result.Err<ConstKey, String> ignored -> {}
            }
        }
        if (inner instanceof TypeObjectDynamic tod && tod.interfaceRepresented().optIsSome()) {
            if (key.value().optIsNone()) {
                return EResult.err(loc, type.typeDesc() + "::field: key has no compile-time value");
            }
            var ckRes = Eval.extractConstKey(xx, key.value().optUnwrap());
            ConstKey ck;
            switch (ckRes) {
                case Result.Ok<ConstKey, String> ok -> ck = ok.value();
                case Result.Err<ConstKey, String> e -> {
                    return EResult.err(loc, e.error());
                }
            }
            ConstKey.Str keyStr;
            switch (ck) {
                case ConstKey.Str s -> keyStr = s;
                case ConstKey.Uint u -> {
                    return EResult.err(loc, "Method " + ck.constKeyToCanonical() + " not found on interface");
                }
            }
            var keyName = keyStr.value();
            for (var m : tod.methods()) {
                if (m.langName().equals(keyName)) {
                    if (!(m.funcType() instanceof TypeMethodDynamic mt)) {
                        return EResult.err(
                                loc,
                                type.typeDesc() + "::field: interface method '" + keyName
                                        + "' funcType is not TypeMethodDynamic");
                    }
                    var refType = new TypeInterfaceMethodBuiltin(tod, keyName, mt);
                    return EResult.ok(
                            new CodeValue(self.code(), EvalOutcome.ofValue(new ValueObjectConst(refType, refType))));
                }
            }
            return EResult.err(loc, "Method '" + keyName + "' not found on interface");
        }
        return EResult.err(
                loc, type.typeDesc() + "::field: no field " + key.value().optUnwrap() + " on " + inner.typeDesc());
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
