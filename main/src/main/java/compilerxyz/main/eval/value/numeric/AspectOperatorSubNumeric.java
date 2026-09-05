package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectOperatorSub;

public final class AspectOperatorSubNumeric implements AspectOperatorSub {
    public static final AspectOperatorSubNumeric INSTANCE = new AspectOperatorSubNumeric();

    private AspectOperatorSubNumeric() {}

    @Override
    public EResult<CodeValue> aspectOperatorSubSub(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.SUB);
    }
}
