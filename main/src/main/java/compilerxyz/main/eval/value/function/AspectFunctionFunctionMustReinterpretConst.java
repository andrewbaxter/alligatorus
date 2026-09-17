package compilerxyz.main.eval.value.function;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.AspectTuple;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Opt;

public record AspectFunctionFunctionMustReinterpretConst(TypeFunctionMustReinterpretConst type)
        implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var erb = new Erb(loc);
        Opt.Some<Value> argSome;
        switch (argument.value()) {
            case Opt.Some<Value> s -> argSome = s;
            case Opt.None<Value> ignored -> {
                return EResult.err(
                        loc, "must_reinterpret: argument must have a compile-time value (a (value, type) tuple)");
            }
        }
        var argValue = argSome.value();
        var argMeta = argValue.valueType();
        var tupleOpt = GenAspectTuple.getMaybe(xx, argMeta);
        Opt.Some<AspectTuple> tupleSome;
        switch (tupleOpt) {
            case Opt.Some<AspectTuple> s -> tupleSome = s;
            case Opt.None<AspectTuple> ignored -> {
                return EResult.err(loc, "must_reinterpret: argument must be a (value, type) tuple");
            }
        }
        var elements = tupleSome.value().aspectTupleTakeElements(xx, loc, argValue);
        if (elements.size() != 2) {
            return EResult.err(
                    loc, "must_reinterpret: expected exactly 2 elements (value, type), got " + elements.size());
        }
        var valRes = elements.get(0);
        if (valRes.hasErrors()) return valRes.reerr();
        var valueCv = valRes.requireValue("must_reinterpret value element");
        var typeRes = elements.get(1);
        if (typeRes.hasErrors()) return typeRes.reerr();
        var typeCv = typeRes.requireValue("must_reinterpret type element");
        Opt.Some<Value> typeSome;
        switch (typeCv.value()) {
            case Opt.Some<Value> s -> typeSome = s;
            case Opt.None<Value> ignored -> {
                return EResult.err(loc, "must_reinterpret: second argument (type) must be constant");
            }
        }
        var typeValue = typeSome.value();
        var typeAtOpt = erb.mustEr(GenAspectType.get(xx, loc, typeValue.valueType()));
        if (typeAtOpt.optIsNone()) return EResult.errs(erb.errors());
        var targetType = typeAtOpt.optUnwrap().aspectTypeType(typeValue);

        Opt.Some<Value> valueSome;
        switch (valueCv.value()) {
            case Opt.Some<Value> s -> valueSome = s;
            case Opt.None<Value> ignored -> {
                return EResult.err(loc, "must_reinterpret: first argument (value) has no compile-time value");
            }
        }
        var sourceType = valueSome.value().valueType();
        var sourceConsumeOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, sourceType));
        if (sourceConsumeOpt.optIsNone()) return EResult.errs(erb.errors());
        var sourceRegOpt = erb.mustEr(DynUtil.materialize(xx, loc, valueCv));
        if (sourceRegOpt.optIsNone()) return EResult.errs(erb.errors());

        var targetConsumeOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, targetType));
        if (targetConsumeOpt.optIsNone()) return EResult.errs(erb.errors());
        var targetJvmClass = targetConsumeOpt.optUnwrap().aspectConsumeDynamicJvmClass(xx);
        if (!targetJvmClass.value.startsWith("L") || !targetJvmClass.value.endsWith(";")) {
            return EResult.err(
                    loc,
                    "must_reinterpret: target type "
                            + targetType.typeDesc()
                            + " has non-object runtime representation "
                            + targetJvmClass.value
                            + "; only object-shaped targets are supported");
        }
        var b = new JavaBytecodeBuilder(loc);
        var out = b.checkcast(targetJvmClass, b.reg(sourceRegOpt.optUnwrap()));
        return EResult.ok(DynUtil.regToValue(b.code(), out, targetType));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
