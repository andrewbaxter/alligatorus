package compilerxyz.main.eval.value.bool;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.gen.AspectOperatorEq;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

public final class AspectOperatorEqBoolDynamic implements AspectOperatorEq {
    public static final AspectOperatorEqBoolDynamic INSTANCE = new AspectOperatorEqBoolDynamic();

    private AspectOperatorEqBoolDynamic() {}

    @Override
    public EResult<CodeValue> aspectOperatorEqEq(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        if (base.value().optIsNone() || reference.value().optIsNone()) {
            return EResult.err(loc, "bool eq: operand has no value");
        }
        var baseAspRes =
                GenAspectConsumeDynamic.get(xx, loc, base.value().optUnwrap().valueType());
        if (baseAspRes.hasErrors()) return baseAspRes.reerr();
        var baseDyn = baseAspRes.requireValue("bool eq base aspect").aspectConsumeDynamicToDynamic(xx, loc, base);
        if (baseDyn.hasErrors()) return baseDyn.reerr();
        var refAspRes = GenAspectConsumeDynamic.get(
                xx, loc, reference.value().optUnwrap().valueType());
        if (refAspRes.hasErrors()) return refAspRes.reerr();
        var refDyn =
                refAspRes.requireValue("bool eq reference aspect").aspectConsumeDynamicToDynamic(xx, loc, reference);
        if (refDyn.hasErrors()) return refDyn.reerr();
        var differ = new LabelNode(new Label());
        var end = new LabelNode(new Label());
        var seq = new JavaBytecodeSequence();
        seq.add(baseDyn.requireValue("bool eq base dynamic"));
        seq.add(refDyn.requireValue("bool eq reference dynamic"));
        seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.IF_ICMPNE, differ)));
        seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ICONST_1));
        seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.GOTO, end)));
        seq.add(JavaBytecodeUtils.wrapObj(loc, differ));
        seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ICONST_0));
        seq.add(JavaBytecodeUtils.wrapObj(loc, end));
        return EResult.ok(new CodeValue(
                Code.target(new CodeDynamic(loc, seq)),
                EvalOutcome.ofValue(ValueDynamic.stack(xx, new TypeBoolDynamic()))));
    }
}
