package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.AspectTuple;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Opt;

public record AspectFunctionMapConstructorConst(TypeMapConstructorConst type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        Opt.Some<Value> argSome;
        switch (argument.value()) {
            case Opt.Some<Value> s -> argSome = s;
            case Opt.None<Value> n -> {
                return EResult.err(loc, "TypeMapConstructorConst::call: argument must have a compile-time value");
            }
        }
        var argValue = argSome.value();
        var argMeta = argValue.valueType();
        var aspectTupleOpt = GenAspectTuple.getMaybe(xx, argMeta);
        Opt.Some<AspectTuple> tupleSome;
        switch (aspectTupleOpt) {
            case Opt.Some<AspectTuple> s -> tupleSome = s;
            case Opt.None<AspectTuple> n -> {
                return EResult.err(loc, "map() requires a tuple of (key_type, value_type)");
            }
        }
        var elements = tupleSome.value().aspectTupleTakeElements(xx, loc, argValue);
        if (elements.size() != 2) {
            return EResult.err(loc, "map() requires exactly 2 type arguments (key, value), got " + elements.size());
        }
        var keyRes = elements.get(0);
        if (keyRes.hasErrors()) return keyRes.reerr();
        var keyCv = keyRes.requireValue("TypeMapConstructorConst::call key element");
        var valueRes = elements.get(1);
        if (valueRes.hasErrors()) return valueRes.reerr();
        var valueCv = valueRes.requireValue("TypeMapConstructorConst::call value element");
        Opt.Some<Value> keySome;
        switch (keyCv.value()) {
            case Opt.Some<Value> s -> keySome = s;
            case Opt.None<Value> n -> {
                return EResult.err(loc, "TypeMapConstructorConst::call: key slot has no compile-time value");
            }
        }
        Opt.Some<Value> valueSome;
        switch (valueCv.value()) {
            case Opt.Some<Value> s -> valueSome = s;
            case Opt.None<Value> n -> {
                return EResult.err(loc, "TypeMapConstructorConst::call: value slot has no compile-time value");
            }
        }
        var keyValue = keySome.value();
        var valueValue = valueSome.value();
        var keyAtRes = GenAspectType.get(xx, loc, keyValue.valueType());
        if (keyAtRes.hasErrors()) return keyAtRes.reerr();
        var keyType = keyAtRes.requireValue("TypeMapConstructorConst::call key AspectType")
                .aspectTypeType(keyValue);
        var valueAtRes = GenAspectType.get(xx, loc, valueValue.valueType());
        if (valueAtRes.hasErrors()) return valueAtRes.reerr();
        var valueType = valueAtRes
                .requireValue("TypeMapConstructorConst::call value AspectType")
                .aspectTypeType(valueValue);
        var keyBoxedRes = DynUtil.requireBoxedTypeArgument(xx, loc, "map key type", keyType);
        if (keyBoxedRes.hasErrors()) return keyBoxedRes.reerr();
        var valueBoxedRes = DynUtil.requireBoxedTypeArgument(xx, loc, "map value type", valueType);
        if (valueBoxedRes.hasErrors()) return valueBoxedRes.reerr();
        Type mapType = new TypeMapDynamic(keyType, valueType);
        var concatRes = Eval.codeConcat(xx, loc, self.code(), argument.code());
        if (concatRes.hasErrors()) return concatRes.reerr();
        return EResult.ok(new CodeValue(
                concatRes.requireValue("concat"),
                EvalOutcome.ofValue(new ValueObjectConst(ValueObjectConst.plainTypeType(), mapType))));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
