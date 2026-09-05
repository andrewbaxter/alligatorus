package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectOperatorAnd;

public final class AspectOperatorAndNumeric implements AspectOperatorAnd {
    public static final AspectOperatorAndNumeric INSTANCE = new AspectOperatorAndNumeric();

    private AspectOperatorAndNumeric() {}

    @Override
    public EResult<CodeValue> aspectOperatorAndAnd(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.AND);
    }
}
