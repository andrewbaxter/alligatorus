package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1063L, identityId = 4063L)
public interface AspectOperatorNeg extends Aspect {
    EResult<CodeValue> aspectOperatorNegNeg(ExprContext xx, QExprId loc, CodeValue base);
}
