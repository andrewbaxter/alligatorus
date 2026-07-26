package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1053L, identityId = 4053L)
public interface AspectOperatorAnd extends Aspect {
  EResult<CodeValue> aspectOperatorAndAnd(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
