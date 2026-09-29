package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamicReg;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.gen.AspectFunction;
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
        var dynamicElements = new TList<CodeDynamicReg>();
        var elementsCarry = Code.none(loc);
        var argTupleOpt = GenAspectTuple.getMaybe(xx, argType);
        if (argTupleOpt.optIsSome()) {
            elementsCarry = argument.code();
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
        var b = new JavaBytecodeBuilder(loc);
        var selfRes = b.addCode(loc, self.code());
        if (selfRes.optIsSome()) return EResult.errs(selfRes.optUnwrap());
        var carryAdd = b.addCode(loc, elementsCarry);
        if (carryAdd.optIsSome()) return EResult.errs(carryAdd.optUnwrap());
        var list = b.reg(type.listType().emitNew(xx, loc, dynamicElements));
        return EResult.ok(DynUtil.regToValue(b.code(), list, type.listType()));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
