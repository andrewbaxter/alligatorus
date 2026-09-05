package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectOperatorGt;

public final class AspectOperatorGtNumeric implements AspectOperatorGt {
    public static final AspectOperatorGtNumeric INSTANCE = new AspectOperatorGtNumeric();

    private AspectOperatorGtNumeric() {}

    @Override
    public EResult<CodeValue> aspectOperatorGtGt(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.GT);
    }
}
