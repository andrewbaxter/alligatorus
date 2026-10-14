package compilerxyz.main.eval.value.option;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.empty.ValueEmpty;
import compilerxyz.main.gen.AspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Pair;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;

public record AspectFieldTypeTypeOption(OptionDynamic type) implements AspectField {

    private TMap<ConstKey, Type> constructors() {
        var elementType = type.optionDynamicElementType();
        return TMap.ofEntries(
                new Pair<>(new ConstKey.Str("some"), new TypeOptionVariantConstructorDynamic(elementType, true)),
                new Pair<>(new ConstKey.Str("none"), new TypeOptionVariantConstructorDynamic(elementType, false)));
    }

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeTypeOption::field: key has no compile-time value");
        }
        var ckRes = Eval.extractConstKey(xx, key.value().optUnwrap());
        ConstKey ck;
        switch (ckRes) {
            case Result.Ok<ConstKey, String> ok -> ck = ok.value();
            case Result.Err<ConstKey, String> err -> {
                return EResult.err(loc, err.error());
            }
        }
        var ctor = constructors().getOpt(ck);
        if (ctor.optIsNone()) {
            return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on " + type.typeDesc());
        }
        return EResult.ok(new CodeValue(self.code(), EvalOutcome.ofValue(new ValueEmpty(ctor.optUnwrap()))));
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return TList.from(constructors().keySet());
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return constructors().getOpt(key);
    }
}
