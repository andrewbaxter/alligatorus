package compilerxyz.main.eval.value.option;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenOptions;
import compilerxyz.prelude.util.Opt;

public final class AspectFunctionOptionConstructorConst implements AspectFunction {
    public static final AspectFunctionOptionConstructorConst INSTANCE = new AspectFunctionOptionConstructorConst();

    private AspectFunctionOptionConstructorConst() {}

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        if (argument.value().optIsNone()) {
            return EResult.err(loc, "TypeOptionConstructorConst::call: argument must have a compile-time value");
        }
        var argValue = argument.value().optUnwrap();
        if (DynUtil.valueIsRuntime(argValue)) {
            return DynUtil.typeConstructRuntime(xx, loc, self, argument, GenCodegenDescriptors.DYN_RUNTIME_OPTION_TYPE);
        }
        var argMeta = argValue.valueType();
        var aspectTypeRes = GenAspectType.get(xx, loc, argMeta);
        if (aspectTypeRes.hasErrors()) return aspectTypeRes.reerr();
        var elementType = aspectTypeRes
                .requireValue("TypeOptionConstructorConst::call AspectType")
                .aspectTypeType(argValue);
        var optType = GenOptions.optionDynamic(xx, elementType);
        var concatRes = Eval.codeConcat(xx, loc, self.code(), argument.code());
        if (concatRes.hasErrors()) return concatRes.reerr();
        return EResult.ok(new CodeValue(
                concatRes.requireValue("concat"),
                EvalOutcome.ofValue(new ValueObjectConst(new TypeTypeOption(optType), optType))));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
