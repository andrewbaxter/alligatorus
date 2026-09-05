package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectOperatorXor;

public final class AspectOperatorXorNumeric implements AspectOperatorXor {
    public static final AspectOperatorXorNumeric INSTANCE = new AspectOperatorXorNumeric();

    private AspectOperatorXorNumeric() {}

    @Override
    public EResult<CodeValue> aspectOperatorXorXor(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.XOR);
    }
}
