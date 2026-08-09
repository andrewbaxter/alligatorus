package compilerxyz.gen;


import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@BuiltinFull(uniqueId = 1056L, identityId = 4056L)
public interface AspectOperatorShl extends Aspect {
  EResult<CodeValue> aspectOperatorShlShl(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
