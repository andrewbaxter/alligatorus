package compilerxyz.gen;


import compilerxyz.annotations.BuiltinFull;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@BuiltinFull(uniqueId = 1018L, identityId = 4018L)
@BuiltinPath("eval.operator_add")
public interface AspectOperatorAdd extends Aspect {
  EResult<CodeValue> aspectOperatorAddAdd(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
