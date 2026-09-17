package compilerxyz.main.eval.value.bool;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.JumpKey;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.gen.AspectOperatorEq;
import org.objectweb.asm.Opcodes;

public final class AspectOperatorEqBoolDynamic implements AspectOperatorEq {
    public static final AspectOperatorEqBoolDynamic INSTANCE = new AspectOperatorEqBoolDynamic();

    private AspectOperatorEqBoolDynamic() {}

    @Override
    public EResult<CodeValue> aspectOperatorEqEq(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        if (base.value().optIsNone() || reference.value().optIsNone()) {
            return EResult.err(loc, "bool eq: operand has no value");
        }
        var baseRes = DynUtil.materialize(xx, loc, base);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var refRes = DynUtil.materialize(xx, loc, reference);
        if (refRes.hasErrors()) return refRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var baseReg = b.reg(baseRes.requireValue("eq base"));
        var refReg = b.reg(refRes.requireValue("eq reference"));
        var differ = new JumpKey();
        var end = new JumpKey();
        b.branch(Opcodes.IF_ICMPNE, differ, baseReg, refReg);
        b.jump(end, b.constBool(true));
        b.land(differ);
        b.jump(end, b.constBool(false));
        var resultType = new TypeBoolDynamic();
        var out = new JavaBytecodeReg(JvmSort.descriptor(xx, resultType));
        b.land(end, out);
        return EResult.ok(DynUtil.regToValue(b.code(), out, resultType));
    }
}
