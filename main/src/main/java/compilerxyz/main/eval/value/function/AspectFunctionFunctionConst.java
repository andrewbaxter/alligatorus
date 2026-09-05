package compilerxyz.main.eval.value.function;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFunctionFunctionConst(TypeFunctionConst type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var argType = type.arg();
        var argList = new TList<CodeDynamic>();
        EResult<TList<CodeDynamic>> argsDynRes;
        if (DynUtil.zeroArgSignature(xx, argType)) {
            argsDynRes = EResult.ok(new TList<>());
        } else {
            var convertedRes = Eval.typecheckConvert(xx, loc, argType, argument);
            if (convertedRes.hasErrors()) return convertedRes.reerr();
            var converted = convertedRes.requireValue("TypeFunctionConst::call converted arg");
            argsDynRes = DynUtil.destructurePositional(xx, loc, argType, converted);
            if (argsDynRes.hasErrors()) return argsDynRes.reerr();
        }
        for (var c : argsDynRes.requireValue("TypeFunctionConst::call positional args")) {
            argList.add(c);
        }
        var descRes = TypeFunctionConst.descriptorFor(xx, loc, type.arg(), type.ret(), type.needsEvalContext());
        if (descRes.hasErrors()) return descRes.reerr();
        var invocationRes = TypeFunctionConst.emitStaticInvocation(
                xx,
                loc,
                type.klass(),
                type.methodName(),
                descRes.requireValue("TypeFunctionConst::emit desc"),
                type.needsEvalContext(),
                argList);
        if (invocationRes.hasErrors()) return invocationRes.reerr();
        var invocation = invocationRes.requireValue("TypeFunctionConst::call invocation");
        if (type.ret().optIsNone()) {
            return EResult.ok(new CodeValue(Code.target(invocation), EvalOutcome.NO_VALUE));
        }
        var retStackRes = GenAspectStackValueDynamic.get(xx, loc, type.ret().optUnwrap());
        if (retStackRes.hasErrors()) return retStackRes.reerr();
        return EResult.ok(retStackRes
                .requireValue("TypeFunctionConst::call ret stack")
                .aspectStackValueDynamicToValue(xx, loc, invocation));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
