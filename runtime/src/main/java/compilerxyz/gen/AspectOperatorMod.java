package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1042L)
@BuiltinPath("eval.operator_mod")
public interface AspectOperatorMod extends Aspect {
  EResult<CodeValue> aspectOperatorModMod(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
