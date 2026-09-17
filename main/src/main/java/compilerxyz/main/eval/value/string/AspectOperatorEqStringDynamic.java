package compilerxyz.main.eval.value.string;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.bool.TypeBoolDynamic;
import compilerxyz.main.gen.AspectOperatorEq;
import compilerxyz.prelude.util.TList;

public final class AspectOperatorEqStringDynamic implements AspectOperatorEq {
    public static final AspectOperatorEqStringDynamic INSTANCE = new AspectOperatorEqStringDynamic();

    private AspectOperatorEqStringDynamic() {}

    @Override
    public EResult<CodeValue> aspectOperatorEqEq(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        if (base.value().optIsNone() || reference.value().optIsNone()) {
            return EResult.err(loc, "string eq: operand has no value");
        }
        var baseRes = DynUtil.materialize(xx, loc, base);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var refRes = DynUtil.materialize(xx, loc, reference);
        if (refRes.hasErrors()) return refRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var baseReg = b.reg(baseRes.requireValue("eq base"));
        var refReg = b.reg(refRes.requireValue("eq reference"));
        var out = b.invokeVirtual(
                JavaInternalName.fromClass(String.class),
                "equals",
                JavaMethodDescriptor.fromParts(
                        JavaDataDescriptor.DESC_BOOL, TList.of(JavaDataDescriptor.fromClass(Object.class))),
                baseReg,
                refReg);
        return EResult.ok(DynUtil.regToValue(b.code(), out, new TypeBoolDynamic()));
    }
}
