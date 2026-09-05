package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFunctionListSpecializedNewDynamic(TypeListSpecializedNewDynamic type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        if (argument.value().optIsNone()) {
            return EResult.err(loc, "TypeListSpecializedNewDynamic::call: argument has no compile-time value");
        }
        var argValue = argument.value().optUnwrap();
        var argType = argValue.valueType();
        var dynamicElements = new TList<CodeDynamic>();
        var argTupleOpt = GenAspectTuple.getMaybe(xx, argType);
        if (argTupleOpt.optIsSome()) {
            var elements = argTupleOpt.optUnwrap().aspectTupleTakeElements(xx, loc, argValue);
            for (var elemRes : elements) {
                if (elemRes.hasErrors()) return elemRes.reerr();
                var dynRes = type.elementToDynamic(xx, loc, elemRes.requireValue("takeElements"));
                if (dynRes.hasErrors()) return dynRes.reerr();
                dynamicElements.add(dynRes.requireValue("elementToDynamic"));
            }
        } else {
            var dynRes = type.elementToDynamic(xx, loc, argument);
            if (dynRes.hasErrors()) return dynRes.reerr();
            dynamicElements.add(dynRes.requireValue("elementToDynamic"));
        }
        var retStackRes = GenAspectStackValueDynamic.get(xx, loc, type.listType());
        if (retStackRes.hasErrors()) {
            return retStackRes.reerr();
        }
        var seq = new JavaBytecodeSequence();
        var selfDynCast = CodeDynamic.castOpt(loc, self.code());
        if (selfDynCast.hasErrors()) return selfDynCast.reerr();
        var selfDyn = selfDynCast.requireValue("selfDyn");
        if (selfDyn.optIsSome()) seq.add(selfDyn.optUnwrap());
        seq.add(type.listType().emitNew(xx, loc, dynamicElements));
        return EResult.ok(retStackRes
                .requireValue("AspectStackValueDynamic.get")
                .aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
