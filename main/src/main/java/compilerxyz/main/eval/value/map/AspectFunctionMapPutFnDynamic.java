package compilerxyz.main.eval.value.map;

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
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;

public record AspectFunctionMapPutFnDynamic(TypeMapPutFnDynamic type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        if (argument.value().optIsNone()) {
            return EResult.err(loc, "TypeMapPutFnDynamic::call: argument has no compile-time value");
        }
        var argValue = argument.value().optUnwrap();
        var argMeta = argValue.valueType();
        var tupleOpt = GenAspectTuple.getMaybe(xx, argMeta);
        if (tupleOpt.optIsNone()) {
            return EResult.err(loc, "TypeMapPutFnDynamic::call: argument must be a (key, value) tuple");
        }
        var elements = tupleOpt.optUnwrap().aspectTupleTakeElements(xx, loc, argValue);
        if (elements.size() != 2) {
            return EResult.err(
                    loc, "TypeMapPutFnDynamic::call: expected exactly 2 elements (key, value), got " + elements.size());
        }
        var keyRes = elements.get(0);
        if (keyRes.hasErrors()) return keyRes.reerr();
        var valRes = elements.get(1);
        if (valRes.hasErrors()) return valRes.reerr();

        var keyConvertedRes = Eval.typecheckConvert(xx, loc, type.keyType(), keyRes.requireValue("put key"));
        if (keyConvertedRes.hasErrors()) return keyConvertedRes.reerr();
        var keyConverted = keyConvertedRes.requireValue("put key converted");
        var keyDynRes = DynUtil.materialize(xx, loc, keyConverted);
        if (keyDynRes.hasErrors()) return keyDynRes.reerr();
        var valConvertedRes = Eval.typecheckConvert(xx, loc, type.valueType(), valRes.requireValue("put value"));
        if (valConvertedRes.hasErrors()) return valConvertedRes.reerr();
        var valDynRes = DynUtil.materialize(xx, loc, valConvertedRes.requireValue("put value converted"));
        if (valDynRes.hasErrors()) return valDynRes.reerr();
        var selfRes = DynUtil.valueReg(xx, loc, self);
        if (selfRes.hasErrors()) return selfRes.reerr();
        var mapName = JavaInternalName.fromClass(TOrderedMap.class);
        var putDesc = JavaMethodDescriptor.fromParts(
                JavaDataDescriptor.DESC_VOID, TList.of(JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT));
        var b = new JavaBytecodeBuilder(loc);
        var selfReg = b.reg(selfRes.requireValue("selfDyn"));
        var argumentAdd = b.addCode(loc, argument.code());
        if (argumentAdd.optIsSome()) return EResult.errs(argumentAdd.optUnwrap());
        var k = b.reg(keyDynRes.requireValue("toDynamic key"));
        var v = b.reg(valDynRes.requireValue("toDynamic value"));
        b.invokeVirtualVoid(mapName, "put", putDesc, selfReg, k, v);
        return EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.NO_VALUE));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
