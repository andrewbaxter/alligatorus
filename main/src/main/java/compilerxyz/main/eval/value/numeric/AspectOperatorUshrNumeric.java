package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectOperatorUshr;

public final class AspectOperatorUshrNumeric implements AspectOperatorUshr {
    public static final AspectOperatorUshrNumeric INSTANCE = new AspectOperatorUshrNumeric();

    private AspectOperatorUshrNumeric() {}

    @Override
    public EResult<CodeValue> aspectOperatorUshrUshr(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.USHR);
    }
}
