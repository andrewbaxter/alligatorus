package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectOperatorDiv;

public final class AspectOperatorDivNumeric implements AspectOperatorDiv {
    public static final AspectOperatorDivNumeric INSTANCE = new AspectOperatorDivNumeric();

    private AspectOperatorDivNumeric() {}

    @Override
    public EResult<CodeValue> aspectOperatorDivDiv(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.DIV);
    }
}
