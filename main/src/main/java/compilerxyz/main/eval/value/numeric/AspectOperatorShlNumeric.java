package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectOperatorShl;

public final class AspectOperatorShlNumeric implements AspectOperatorShl {
    public static final AspectOperatorShlNumeric INSTANCE = new AspectOperatorShlNumeric();

    private AspectOperatorShlNumeric() {}

    @Override
    public EResult<CodeValue> aspectOperatorShlShl(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.SHL);
    }
}
