package compilerxyz.gen;


import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@BuiltinFull(uniqueId = 1054L, identityId = 4054L)
public interface AspectOperatorOr extends Aspect {
  EResult<CodeValue> aspectOperatorOrOr(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
