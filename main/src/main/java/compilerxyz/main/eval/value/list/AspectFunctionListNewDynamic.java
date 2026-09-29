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
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFunctionListNewDynamic(TypeListNewDynamic type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        if (argument.value().optIsNone()) {
            return EResult.err(loc, "TypeListNewDynamic::call: argument has no compile-time value");
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
                var dynRes = DynUtil.materialize(xx, loc, type.elementType(), elemRes.requireValue("takeElements"));
                if (dynRes.hasErrors()) return dynRes.reerr();
                dynamicElements.add(dynRes.requireValue("toDynamic"));
            }
        } else {
            var dynRes = DynUtil.materialize(xx, loc, type.elementType(), argument);
            if (dynRes.hasErrors()) return dynRes.reerr();
            dynamicElements.add(dynRes.requireValue("toDynamic"));
        }
        Type instanceType = new TypeListBoxedDynamic(type.elementType());
        var tlistIntname = JavaInternalName.fromClass(TList.class);
        var descObject = JavaDataDescriptor.fromClass(Object.class);
        var addDesc = JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID, TList.of(descObject));
        var b = new JavaBytecodeBuilder(loc);
        var carryAdd = b.addCode(loc, elementsCarry);
        if (carryAdd.optIsSome()) return EResult.errs(carryAdd.optUnwrap());
        var regs = new TList<JavaBytecodeReg>(dynamicElements.size());
        for (var element : dynamicElements) regs.add(b.reg(element));
        var selfRes = b.addCode(loc, self.code());
        if (selfRes.optIsSome()) return EResult.errs(selfRes.optUnwrap());
        var list = b.newInstance(tlistIntname, JavaMethodDescriptor.fromConstructorParts(TList.of()));
        for (var reg : regs) b.invokeVirtualVoid(tlistIntname, "add", addDesc, list, reg);
        return EResult.ok(DynUtil.regToValue(b.code(), list, instanceType));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
