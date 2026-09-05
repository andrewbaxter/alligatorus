package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.prelude.util.Opt;

public record AspectFunctionInterfaceMethodBuiltin(TypeInterfaceMethodBuiltin type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var splitRes = DynUtil.methodRefSplitArgument(
                xx, loc, type.method().arg(), argument, "interface method '" + type.name() + "'");
        if (splitRes.hasErrors()) return splitRes.reerr();
        var split = splitRes.requireValue("TypeInterfaceMethodBuiltin split");
        var receiverRes = Eval.typecheckConvert(xx, loc, type.receiverType(), split.receiver());
        if (receiverRes.hasErrors()) return receiverRes.reerr();
        var receiverCv = receiverRes.requireValue("TypeInterfaceMethodBuiltin receiver");
        var receiverDynRes = GenAspectConsumeDynamic.getMaybe(xx, type.receiverType())
                .optUnwrap()
                .aspectConsumeDynamicToDynamic(xx, loc, receiverCv);
        if (receiverDynRes.hasErrors()) return receiverDynRes.reerr();
        var receiverDyn = receiverDynRes.requireValue("TypeInterfaceMethodBuiltin receiver toDynamic");
        return GenAspectFunction.getMaybe(xx, type.method())
                .optUnwrap()
                .aspectFunctionCall(
                        xx, loc, new CodeValue(Code.target(receiverDyn), receiverCv.outcome()), split.methodArgs());
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
