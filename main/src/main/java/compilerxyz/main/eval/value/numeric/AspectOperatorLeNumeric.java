package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectOperatorLe;

public final class AspectOperatorLeNumeric implements AspectOperatorLe {
    public static final AspectOperatorLeNumeric INSTANCE = new AspectOperatorLeNumeric();

    private AspectOperatorLeNumeric() {}

    @Override
    public EResult<CodeValue> aspectOperatorLeLe(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.LE);
    }
}
