package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectOperatorOr;

public final class AspectOperatorOrNumeric implements AspectOperatorOr {
    public static final AspectOperatorOrNumeric INSTANCE = new AspectOperatorOrNumeric();

    private AspectOperatorOrNumeric() {}

    @Override
    public EResult<CodeValue> aspectOperatorOrOr(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.OR);
    }
}
