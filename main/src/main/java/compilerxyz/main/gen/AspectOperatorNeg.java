package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 1063L, identityId = 4063L)
@BuiltinPath("eval.operator_neg")
public interface AspectOperatorNeg extends Aspect {
    EResult<CodeValue> aspectOperatorNegNeg(ExprContext xx, QExprId loc, CodeValue base);
}
