package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectOperatorNeg;

public final class AspectOperatorNegNumeric implements AspectOperatorNeg {
    public static final AspectOperatorNegNumeric INSTANCE = new AspectOperatorNegNumeric();

    private AspectOperatorNegNumeric() {}

    @Override
    public EResult<CodeValue> aspectOperatorNegNeg(ExprContext xx, QExprId loc, CodeValue base) {
        return Numerics.unary(xx, loc, base, Numerics.Unary.NEG);
    }
}
