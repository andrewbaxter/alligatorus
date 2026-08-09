package compilerxyz.gen;


import compilerxyz.annotations.BuiltinFull;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@BuiltinFull(uniqueId = 1042L, identityId = 4042L)
@BuiltinPath("eval.operator_mod")
public interface AspectOperatorMod extends Aspect {
  EResult<CodeValue> aspectOperatorModMod(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
