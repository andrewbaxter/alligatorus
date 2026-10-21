package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.value.int_.TypeIntDynamic;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFunctionListSetDynamic(TypeListSetDynamic type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        if (argument.value().optIsNone()) {
            return EResult.err(loc, "TypeListSetDynamic::call: argument has no compile-time value");
        }
        var argValue = argument.value().optUnwrap();
        var tupleOpt = GenAspectTuple.getMaybe(xx, argValue.valueType());
        if (tupleOpt.optIsNone()) {
            return EResult.err(loc, "TypeListSetDynamic::call: argument must be an (index, value) tuple");
        }
        var elements = tupleOpt.optUnwrap().aspectTupleTakeElements(xx, loc, argValue);
        if (elements.size() != 2) {
            return EResult.err(
                    loc,
                    "TypeListSetDynamic::call: expected exactly 2 elements (index, value), got " + elements.size());
        }
        var indexRes = elements.get(0);
        if (indexRes.hasErrors()) return indexRes.reerr();
        var valueRes = elements.get(1);
        if (valueRes.hasErrors()) return valueRes.reerr();
        var indexConvertedRes =
                Eval.typecheckConvert(xx, loc, new TypeIntDynamic(), indexRes.requireValue("set index"));
        if (indexConvertedRes.hasErrors()) return indexConvertedRes.reerr();
        var indexDynRes = DynUtil.materialize(xx, loc, indexConvertedRes.requireValue("set index converted"));
        if (indexDynRes.hasErrors()) return indexDynRes.reerr();
        Type elementType =
                switch (type.listType()) {
                    case TypeListSpecializedDynamic<?> specialized -> specialized.elementType;
                    case TypeListBoxedDynamic<?> boxed -> boxed.elementType;
                };
        var valueConvertedRes = Eval.typecheckConvert(xx, loc, elementType, valueRes.requireValue("set value"));
        if (valueConvertedRes.hasErrors()) return valueConvertedRes.reerr();
        var valueDynRes = DynUtil.materialize(xx, loc, valueConvertedRes.requireValue("set value converted"));
        if (valueDynRes.hasErrors()) return valueDynRes.reerr();
        var selfRes = DynUtil.valueReg(xx, loc, self);
        if (selfRes.hasErrors()) return selfRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var list = b.reg(selfRes.requireValue("selfDyn"));
        var argumentAdd = b.addCode(loc, argument.code());
        if (argumentAdd.optIsSome()) return EResult.errs(argumentAdd.optUnwrap());
        var index = b.reg(indexDynRes.requireValue("toDynamic index"));
        var value = b.reg(valueDynRes.requireValue("toDynamic value"));
        switch (type.listType()) {
            case TypeListSpecializedDynamic<?> specialized -> {
                var setDesc = JavaMethodDescriptor.fromParts(
                        JavaDataDescriptor.DESC_VOID,
                        TList.of(JavaDataDescriptor.DESC_INT, specialized.elementDescriptor(xx)));
                b.invokeVirtualVoid(specialized.storeName, "set", setDesc, list, index, value);
            }
            case TypeListBoxedDynamic<?> boxed -> {
                var setDesc = JavaMethodDescriptor.fromParts(
                        JavaDataDescriptor.DESC_VOID, TList.of(JavaDataDescriptor.DESC_INT, JvmConstants.DESC_OBJECT));
                b.invokeVirtualVoid(JavaInternalName.fromClass(TList.class), "set", setDesc, list, index, value);
            }
        }
        return EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.NO_VALUE));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
