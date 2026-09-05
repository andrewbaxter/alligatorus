package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectOperatorMod;

public final class AspectOperatorModNumeric implements AspectOperatorMod {
    public static final AspectOperatorModNumeric INSTANCE = new AspectOperatorModNumeric();

    private AspectOperatorModNumeric() {}

    @Override
    public EResult<CodeValue> aspectOperatorModMod(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.MOD);
    }
}
