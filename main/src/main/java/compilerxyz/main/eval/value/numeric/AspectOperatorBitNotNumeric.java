package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectOperatorBitNot;

public final class AspectOperatorBitNotNumeric implements AspectOperatorBitNot {
    public static final AspectOperatorBitNotNumeric INSTANCE = new AspectOperatorBitNotNumeric();

    private AspectOperatorBitNotNumeric() {}

    @Override
    public EResult<CodeValue> aspectOperatorBitNotBitNot(ExprContext xx, QExprId loc, CodeValue base) {
        return Numerics.unary(xx, loc, base, Numerics.Unary.BITNOT);
    }
}
