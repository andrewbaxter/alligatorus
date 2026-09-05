package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectOperatorMul;

public final class AspectOperatorMulNumeric implements AspectOperatorMul {
    public static final AspectOperatorMulNumeric INSTANCE = new AspectOperatorMulNumeric();

    private AspectOperatorMulNumeric() {}

    @Override
    public EResult<CodeValue> aspectOperatorMulMul(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.MUL);
    }
}
