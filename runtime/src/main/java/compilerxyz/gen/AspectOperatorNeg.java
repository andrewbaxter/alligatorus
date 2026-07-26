package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1063L, identityId = 4063L)
public interface AspectOperatorNeg extends Aspect {
  EResult<CodeValue> aspectOperatorNegNeg(ExprContext xx, QExprId loc, CodeValue base);
}
