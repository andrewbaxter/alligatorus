package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectOperatorShr;

public final class AspectOperatorShrNumeric implements AspectOperatorShr {
    public static final AspectOperatorShrNumeric INSTANCE = new AspectOperatorShrNumeric();

    private AspectOperatorShrNumeric() {}

    @Override
    public EResult<CodeValue> aspectOperatorShrShr(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.SHR);
    }
}
