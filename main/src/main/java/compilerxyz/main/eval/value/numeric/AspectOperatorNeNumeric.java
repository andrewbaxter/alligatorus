package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectOperatorNe;

public final class AspectOperatorNeNumeric implements AspectOperatorNe {
    public static final AspectOperatorNeNumeric INSTANCE = new AspectOperatorNeNumeric();

    private AspectOperatorNeNumeric() {}

    @Override
    public EResult<CodeValue> aspectOperatorNeNe(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.NE);
    }
}
