package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@BuiltinAspect(uniqueId = 1018L, kind = "OperatorAdd")
public interface AspectOperatorAdd extends Aspect {
  EResult<CodeValue> aspectOperatorAddAdd(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
