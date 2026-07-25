package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1018L)
@BuiltinPath("eval.operator_add")
public interface AspectOperatorAdd extends Aspect {
  EResult<CodeValue> aspectOperatorAddAdd(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
