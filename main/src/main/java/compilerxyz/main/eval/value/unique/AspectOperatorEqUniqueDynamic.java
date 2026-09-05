package compilerxyz.main.eval.value.unique;

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
import compilerxyz.main.eval.value.bool.TypeBoolConst;
import compilerxyz.main.eval.value.bool.TypeBoolDynamic;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectOperatorEq;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.prelude.util.TList;

public final class AspectOperatorEqUniqueDynamic implements AspectOperatorEq {
    public static final AspectOperatorEqUniqueDynamic INSTANCE = new AspectOperatorEqUniqueDynamic();

    private AspectOperatorEqUniqueDynamic() {}

    @Override
    public EResult<CodeValue> aspectOperatorEqEq(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        var b = TypeUniqueDynamic.extractUnique(base);
        var r = TypeUniqueDynamic.extractUnique(reference);
        if (b.optIsSome() && r.optIsSome()) {
            return EResult.ok(new CodeValue(
                    Code.none(loc),
                    EvalOutcome.ofValue(new ValueObjectConst(
                            new TypeBoolConst(), b.optUnwrap().equals(r.optUnwrap())))));
        }
        if (base.value().optIsNone() || reference.value().optIsNone()) {
            return EResult.err(loc, "TypeUniqueDynamic::eq: operand has no value");
        }
        var baseAspRes =
                GenAspectConsumeDynamic.get(xx, loc, base.value().optUnwrap().valueType());
        if (baseAspRes.hasErrors()) return baseAspRes.reerr();
        var baseDyn = baseAspRes
                .requireValue("TypeUniqueDynamic::eq base aspect")
                .aspectConsumeDynamicToDynamic(xx, loc, base);
        if (baseDyn.hasErrors()) return EResult.err(loc, "TypeUniqueDynamic::eq: base lowering failed");
        var refAspRes = GenAspectConsumeDynamic.get(
                xx, loc, reference.value().optUnwrap().valueType());
        if (refAspRes.hasErrors()) return refAspRes.reerr();
        var refDyn = refAspRes
                .requireValue("TypeUniqueDynamic::eq ref aspect")
                .aspectConsumeDynamicToDynamic(xx, loc, reference);
        if (refDyn.hasErrors()) return EResult.err(loc, "TypeUniqueDynamic::eq: reference lowering failed");
        var eueBase = baseDyn.requireValue("TypeUniqueDynamic::eq base dynamic");
        var ref = refDyn.requireValue("TypeUniqueDynamic::eq reference dynamic");
        var seq = new JavaBytecodeSequence();
        seq.add(eueBase);
        seq.add(ref);
        seq.add(JavaBytecodeUtils.callMethod(
                loc,
                JavaInternalName.fromClass(Object.class),
                "equals",
                JavaMethodDescriptor.fromParts(
                        JavaDataDescriptor.DESC_BOOL, TList.of(JavaDataDescriptor.fromClass(Object.class)))));
        var call = new CodeDynamic(loc, seq);
        return EResult.ok(
                new CodeValue(Code.target(call), EvalOutcome.ofValue(ValueDynamic.stack(xx, new TypeBoolDynamic()))));
    }
}
