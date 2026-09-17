package compilerxyz.main.eval.value.unique;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.bool.TypeBoolConst;
import compilerxyz.main.eval.value.bool.TypeBoolDynamic;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectOperatorEq;
import compilerxyz.prelude.util.TList;

public final class AspectOperatorEqUniqueDynamic implements AspectOperatorEq {
    public static final AspectOperatorEqUniqueDynamic INSTANCE = new AspectOperatorEqUniqueDynamic();

    private AspectOperatorEqUniqueDynamic() {}

    @Override
    public EResult<CodeValue> aspectOperatorEqEq(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        var bu = TypeUniqueDynamic.extractUnique(base);
        var ru = TypeUniqueDynamic.extractUnique(reference);
        if (bu.optIsSome() && ru.optIsSome()) {
            return EResult.ok(new CodeValue(
                    Code.none(loc),
                    EvalOutcome.ofValue(new ValueObjectConst(
                            new TypeBoolConst(), bu.optUnwrap().equals(ru.optUnwrap())))));
        }
        if (base.value().optIsNone() || reference.value().optIsNone()) {
            return EResult.err(loc, "TypeUniqueDynamic::eq: operand has no value");
        }
        var baseRes = DynUtil.materialize(xx, loc, base);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var refRes = DynUtil.materialize(xx, loc, reference);
        if (refRes.hasErrors()) return refRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var baseReg = b.reg(baseRes.requireValue("eq base"));
        var refReg = b.reg(refRes.requireValue("eq reference"));
        var out = b.invokeVirtual(
                JavaInternalName.fromClass(Object.class),
                "equals",
                JavaMethodDescriptor.fromParts(
                        JavaDataDescriptor.DESC_BOOL, TList.of(JavaDataDescriptor.fromClass(Object.class))),
                baseReg,
                refReg);
        return EResult.ok(DynUtil.regToValue(b.code(), out, new TypeBoolDynamic()));
    }
}
