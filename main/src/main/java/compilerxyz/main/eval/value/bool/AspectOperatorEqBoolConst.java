package compilerxyz.main.eval.value.bool;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectOperatorEq;
import compilerxyz.main.gen.GenAspectBoolConst;

public final class AspectOperatorEqBoolConst implements AspectOperatorEq {
    public static final AspectOperatorEqBoolConst INSTANCE = new AspectOperatorEqBoolConst();

    private AspectOperatorEqBoolConst() {}

    @Override
    public EResult<CodeValue> aspectOperatorEqEq(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        if (base.value().optIsNone() || reference.value().optIsNone()) {
            return EResult.err(loc, "bool eq: operand has no value");
        }
        var baseValue = base.value().optUnwrap();
        var refValue = reference.value().optUnwrap();
        var baseConst = GenAspectBoolConst.getMaybe(xx, baseValue.valueType());
        var refConst = GenAspectBoolConst.getMaybe(xx, refValue.valueType());
        if (baseConst.optIsSome() && refConst.optIsSome()) {
            var concatRes = Eval.codeConcat(xx, loc, base.code(), reference.code());
            if (concatRes.hasErrors()) return concatRes.reerr();
            var equal = baseConst.optUnwrap().aspectBoolConstGet(baseValue)
                    == refConst.optUnwrap().aspectBoolConstGet(refValue);
            return EResult.ok(new CodeValue(
                    concatRes.requireValue("concat"),
                    EvalOutcome.ofValue(new ValueObjectConst(new TypeBoolConst(), equal))));
        }
        return AspectOperatorEqBoolDynamic.INSTANCE.aspectOperatorEqEq(xx, loc, base, reference);
    }
}
