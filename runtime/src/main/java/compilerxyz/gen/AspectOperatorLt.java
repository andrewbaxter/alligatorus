package compilerxyz.gen;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@BuiltinAspect(uniqueId = 1020L, kind = "OperatorLt")
@compilerxyz.annotations.BuiltinTypeId(7228L)
public interface AspectOperatorLt extends Aspect {
  EResult<CodeValue> aspectOperatorLtLt(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
