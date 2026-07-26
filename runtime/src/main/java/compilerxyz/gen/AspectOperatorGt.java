package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1061L, identityId = 4061L)
public interface AspectOperatorGt extends Aspect {
  EResult<CodeValue> aspectOperatorGtGt(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
