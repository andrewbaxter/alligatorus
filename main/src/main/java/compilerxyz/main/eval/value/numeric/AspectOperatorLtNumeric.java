package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectOperatorLt;

public final class AspectOperatorLtNumeric implements AspectOperatorLt {
    public static final AspectOperatorLtNumeric INSTANCE = new AspectOperatorLtNumeric();

    private AspectOperatorLtNumeric() {}

    @Override
    public EResult<CodeValue> aspectOperatorLtLt(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.LT);
    }
}
