package compilerxyz.main.eval.value.option;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenOptions;
import compilerxyz.prelude.util.Opt;

public record AspectConsumeDynamicOptionConst<T extends Type>(TypeOptionConst<T> type) implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return GenAspectConsumeDynamic.getMaybe(xx, GenOptions.optionDynamic(xx, type.elementType))
                .optUnwrap()
                .aspectConsumeDynamicJvmClass(xx);
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeOptionConst::toDynamic: CodeValue has no compile-time value");
        }
        var payload = TypeOptionConst.payloadOf(cv.value().optUnwrap());
        var dynamic = GenOptions.optionDynamic(xx, type.elementType);
        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, cv.code());
        if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
        Opt<JavaBytecodeReg> payloadReg = Opt.none();
        if (payload.optIsSome()) {
            var payloadRes = DynUtil.materialize(
                    xx, loc, type.elementType, new CodeValue(Code.none(loc), EvalOutcome.ofValue(payload.optUnwrap())));
            if (payloadRes.hasErrors()) return payloadRes.reerr();
            payloadReg = Opt.some(b.reg(payloadRes.requireValue("TypeOptionConst::toDynamic payload")));
        }
        var out = b.reg(dynamic.optionDynamicConstruct(xx, loc, payloadReg));
        return EResult.ok(DynUtil.regToValue(b.code(), out, dynamic));
    }
}
