package compilerxyz.main.eval.value.function;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamicReg;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.gen.AspectTuple;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

record ReinterpretArgs(CodeDynamicReg value, Type targetType, JavaDataDescriptor targetJvmClass) {

    static EResult<TList<CodeValue>> elements(
            ExprContext xx, QExprId loc, String name, String shape, CodeValue argument) {
        Opt.Some<Value> argSome;
        switch (argument.value()) {
            case Opt.Some<Value> s -> argSome = s;
            case Opt.None<Value> ignored -> {
                return EResult.err(loc, name + ": argument must have a compile-time value (a " + shape + " tuple)");
            }
        }
        var argValue = argSome.value();
        Opt.Some<AspectTuple> tupleSome;
        switch (GenAspectTuple.getMaybe(xx, argValue.valueType())) {
            case Opt.Some<AspectTuple> s -> tupleSome = s;
            case Opt.None<AspectTuple> ignored -> {
                return EResult.err(loc, name + ": argument must be a " + shape + " tuple");
            }
        }
        var arity = shape.split(",").length;
        var elements = tupleSome.value().aspectTupleTakeElements(xx, loc, argValue);
        if (elements.size() != arity) {
            return EResult.err(
                    loc, name + ": expected exactly " + arity + " elements " + shape + ", got " + elements.size());
        }
        var out = new TList<CodeValue>(arity);
        for (var element : elements) {
            if (element.hasErrors()) return element.reerr();
            out.add(element.requireValue(name + " element"));
        }
        return EResult.ok(out);
    }

    static EResult<ReinterpretArgs> parse(
            ExprContext xx, QExprId loc, String name, CodeValue valueCv, CodeValue typeCv) {
        var erb = new Erb(loc);
        Opt.Some<Value> typeSome;
        switch (typeCv.value()) {
            case Opt.Some<Value> s -> typeSome = s;
            case Opt.None<Value> ignored -> {
                return EResult.err(loc, name + ": type must be constant");
            }
        }
        var typeValue = typeSome.value();
        var typeAtOpt = erb.mustEr(GenAspectType.get(xx, loc, typeValue.valueType()));
        if (typeAtOpt.optIsNone()) return EResult.errs(erb.errors());
        var targetType = typeAtOpt.optUnwrap().aspectTypeType(typeValue);

        if (valueCv.value().optIsNone()) {
            return EResult.err(loc, name + ": value has no compile-time value");
        }
        var sourceRegOpt = erb.mustEr(DynUtil.materialize(xx, loc, valueCv));
        if (sourceRegOpt.optIsNone()) return EResult.errs(erb.errors());

        var targetConsumeOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, targetType));
        if (targetConsumeOpt.optIsNone()) return EResult.errs(erb.errors());
        var targetJvmClass = targetConsumeOpt.optUnwrap().aspectConsumeDynamicJvmClass(xx);
        if (!targetJvmClass.isObject()) {
            return EResult.err(
                    loc,
                    name
                            + ": target type "
                            + targetType.typeDesc()
                            + " has non-object runtime representation "
                            + targetJvmClass.value
                            + "; only object-shaped targets are supported");
        }
        return EResult.ok(new ReinterpretArgs(sourceRegOpt.optUnwrap(), targetType, targetJvmClass));
    }
}
