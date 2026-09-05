package compilerxyz.main.eval.value.source_function;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DefSetFunctionsEmitter;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.function.TypeFunctionConst;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.prelude.util.Opt;

public record AspectFunctionSourceFunctionConst(TypeSourceFunctionConst type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue function, CodeValue argument) {
        var set = type.data().set;
        if (set == null) {
            return EResult.err(loc, "TypeSourceFunctionConst::call: definition set function has no set reference");
        }
        xx.vx.definitionSets.putIfAbsent(set.defSetId, set);
        if (type.data().argType.optIsNone()) {
            return EResult.err(loc, "TypeSourceFunctionConst::call: definition set function missing arg type");
        }
        var argType = type.data().argType.optUnwrap();
        var retType = type.data().retType;
        var klass = JavaInternalName.of(DefSetFunctionsEmitter.functionsInternalName(set.defSetId));
        var methodName = type.data().jvmMethodName;
        var convertedRes = Eval.typecheckConvert(xx, loc, argType, argument);
        if (convertedRes.hasErrors()) return convertedRes.reerr();
        var converted = convertedRes.requireValue("TypeSourceFunctionConst::call converted arg");
        var argCodesRes = DynUtil.destructurePositional(xx, loc, argType, converted);
        if (argCodesRes.hasErrors()) return argCodesRes.reerr();
        var argCodes = argCodesRes.requireValue("TypeSourceFunctionConst::call arg codes");

        var descRes = TypeFunctionConst.descriptorFor(xx, loc, argType, retType, false);
        if (descRes.hasErrors()) return descRes.reerr();
        var invocationRes = TypeFunctionConst.emitStaticInvocation(
                xx,
                loc,
                klass,
                methodName,
                descRes.requireValue("TypeSourceFunctionConst::call descriptor"),
                false,
                argCodes);
        if (invocationRes.hasErrors()) return invocationRes.reerr();
        var invocation = invocationRes.requireValue("TypeSourceFunctionConst::call invocation");
        if (retType.optIsNone()) {
            return EResult.ok(new CodeValue(Code.target(invocation), EvalOutcome.NO_VALUE));
        }
        var retStackRes = GenAspectStackValueDynamic.get(xx, loc, retType.optUnwrap());
        if (retStackRes.hasErrors()) return retStackRes.reerr();
        return EResult.ok(retStackRes
                .requireValue("TypeSourceFunctionConst::call ret stack")
                .aspectStackValueDynamicToValue(xx, loc, invocation));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return type.data().exceptionType;
    }
}
