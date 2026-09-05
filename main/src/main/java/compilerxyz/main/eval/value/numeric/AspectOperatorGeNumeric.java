package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectOperatorGe;

public final class AspectOperatorGeNumeric implements AspectOperatorGe {
    public static final AspectOperatorGeNumeric INSTANCE = new AspectOperatorGeNumeric();

    private AspectOperatorGeNumeric() {}

    @Override
    public EResult<CodeValue> aspectOperatorGeGe(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.GE);
    }
}
