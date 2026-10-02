package compilerxyz.main.eval.value.eresult;

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
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.value.list.TypeListConst;
import compilerxyz.main.eval.value.list.ValueListConst;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.GenExprErr;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;

public record AspectConsumeDynamicEResultConst<T extends Type>(TypeEResultConst<T> type)
        implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(EResult.class));
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeEResultConst::toDynamic: CodeValue has no compile-time value");
        }
        var eresultName = JavaInternalName.fromClass(EResult.class);
        var eresultDesc = JavaDataDescriptor.fromInternalName(eresultName);
        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, cv.code());
        if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
        JavaBytecodeReg out;
        switch (TypeEResultConst.resultOf(cv.value().optUnwrap())) {
            case Result.Ok<Value, TList<Value>> ok -> {
                var valueRes = DynUtil.materialize(
                        xx, loc, type.elementType, new CodeValue(Code.none(loc), EvalOutcome.ofValue(ok.value())));
                if (valueRes.hasErrors()) return valueRes.reerr();
                out = b.invokeStatic(
                        eresultName,
                        "ok",
                        JavaMethodDescriptor.fromParts(eresultDesc, TList.of(JvmConstants.DESC_OBJECT)),
                        b.reg(valueRes.requireValue("TypeEResultConst::toDynamic ok")));
            }
            case Result.Err<Value, TList<Value>> err -> {
                var errsCv = new CodeValue(
                        Code.none(loc),
                        EvalOutcome.ofValue(
                                new ValueListConst<>(new TypeListConst<>(GenExprErr.dynamicType()), err.error())));
                var errsRes = DynUtil.materialize(xx, loc, errsCv);
                if (errsRes.hasErrors()) return errsRes.reerr();
                out = b.invokeStatic(
                        eresultName,
                        "errs",
                        JavaMethodDescriptor.fromParts(
                                eresultDesc,
                                TList.of(JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(TList.class)))),
                        b.reg(errsRes.requireValue("TypeEResultConst::toDynamic errs")));
            }
        }
        return EResult.ok(DynUtil.regToValue(b.code(), out, new TypeEResultDynamic<>(type.elementType)));
    }
}
