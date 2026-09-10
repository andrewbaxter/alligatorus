package compilerxyz.main.eval.value.string;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.bool.TypeBoolDynamic;
import compilerxyz.main.gen.AspectOperatorEq;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.prelude.util.TList;

public final class AspectOperatorEqStringDynamic implements AspectOperatorEq {
    public static final AspectOperatorEqStringDynamic INSTANCE = new AspectOperatorEqStringDynamic();

    private AspectOperatorEqStringDynamic() {}

    @Override
    public EResult<CodeValue> aspectOperatorEqEq(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        if (base.value().optIsNone() || reference.value().optIsNone()) {
            return EResult.err(loc, "string eq: operand has no value");
        }
        var baseAspRes =
                GenAspectConsumeDynamic.get(xx, loc, base.value().optUnwrap().valueType());
        if (baseAspRes.hasErrors()) return baseAspRes.reerr();
        var baseDyn = baseAspRes.requireValue("string eq base aspect").aspectConsumeDynamicToDynamic(xx, loc, base);
        if (baseDyn.hasErrors()) return baseDyn.reerr();
        var refAspRes = GenAspectConsumeDynamic.get(
                xx, loc, reference.value().optUnwrap().valueType());
        if (refAspRes.hasErrors()) return refAspRes.reerr();
        var refDyn =
                refAspRes.requireValue("string eq reference aspect").aspectConsumeDynamicToDynamic(xx, loc, reference);
        if (refDyn.hasErrors()) return refDyn.reerr();
        var seq = new JavaBytecodeSequence();
        seq.add(baseDyn.requireValue("string eq base dynamic"));
        seq.add(refDyn.requireValue("string eq reference dynamic"));
        seq.add(JavaBytecodeUtils.callMethod(
                loc,
                JavaInternalName.fromClass(String.class),
                "equals",
                JavaMethodDescriptor.fromParts(
                        JavaDataDescriptor.DESC_BOOL, TList.of(JavaDataDescriptor.fromClass(Object.class)))));
        return EResult.ok(new CodeValue(
                Code.target(new CodeDynamic(loc, seq)),
                EvalOutcome.ofValue(ValueDynamic.stack(xx, new TypeBoolDynamic()))));
    }
}
