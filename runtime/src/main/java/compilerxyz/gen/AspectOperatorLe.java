package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1060L, identityId = 4060L)
public interface AspectOperatorLe extends Aspect {
  EResult<CodeValue> aspectOperatorLeLe(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
