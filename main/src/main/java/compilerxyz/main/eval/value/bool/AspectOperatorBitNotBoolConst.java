package compilerxyz.main.eval.value.bool;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectOperatorBitNot;
import compilerxyz.main.gen.GenAspectBoolConst;

public final class AspectOperatorBitNotBoolConst implements AspectOperatorBitNot {
    public static final AspectOperatorBitNotBoolConst INSTANCE = new AspectOperatorBitNotBoolConst();

    private AspectOperatorBitNotBoolConst() {}

    @Override
    public EResult<CodeValue> aspectOperatorBitNotBitNot(ExprContext xx, QExprId loc, CodeValue base) {
        if (base.value().optIsNone()) {
            return EResult.err(loc, "bool not: operand has no value");
        }
        var baseValue = base.value().optUnwrap();
        var negated = !GenAspectBoolConst.getMaybe(xx, baseValue.valueType())
                .optUnwrap()
                .aspectBoolConstGet(baseValue);
        return EResult.ok(
                new CodeValue(base.code(), EvalOutcome.ofValue(new ValueObjectConst(new TypeBoolConst(), negated))));
    }
}
