package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectOperatorEq;

public final class AspectOperatorEqNumeric implements AspectOperatorEq {
    public static final AspectOperatorEqNumeric INSTANCE = new AspectOperatorEqNumeric();

    private AspectOperatorEqNumeric() {}

    @Override
    public EResult<CodeValue> aspectOperatorEqEq(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.EQ);
    }
}
