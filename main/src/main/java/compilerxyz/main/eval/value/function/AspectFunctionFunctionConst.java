package compilerxyz.main.eval.value.function;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamicReg;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFunctionFunctionConst(TypeFunctionConst type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var argType = type.arg();
        var argList = new TList<CodeDynamicReg>();
        EResult<TList<CodeDynamicReg>> argsDynRes;
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
        var b = new JavaBytecodeBuilder(loc);
        var invocation = DynUtil.invokeStaticFunction(
                xx,
                loc,
                b,
                type.klass(),
                type.methodName(),
                descRes.requireValue("TypeFunctionConst::emit desc"),
                type.needsEvalContext(),
                argList);
        if (type.ret().optIsNone() || invocation.optIsNone()) {
            return EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.NO_VALUE));
        }
        return EResult.ok(DynUtil.regToTypedValue(
                xx, loc, b.code(), invocation.optUnwrap(), type.ret().optUnwrap()));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
