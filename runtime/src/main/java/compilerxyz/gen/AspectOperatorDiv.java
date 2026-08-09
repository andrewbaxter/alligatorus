package compilerxyz.gen;


import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@BuiltinFull(uniqueId = 1052L, identityId = 4052L)
public interface AspectOperatorDiv extends Aspect {
  EResult<CodeValue> aspectOperatorDivDiv(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
