package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectOperatorAdd;

public final class AspectOperatorAddNumeric implements AspectOperatorAdd {
    public static final AspectOperatorAddNumeric INSTANCE = new AspectOperatorAddNumeric();

    private AspectOperatorAddNumeric() {}

    @Override
    public EResult<CodeValue> aspectOperatorAddAdd(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.ADD);
    }
}
