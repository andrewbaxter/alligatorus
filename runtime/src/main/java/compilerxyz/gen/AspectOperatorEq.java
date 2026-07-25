package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1019L)
public interface AspectOperatorEq extends Aspect {
  EResult<CodeValue> aspectOperatorEqEq(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
