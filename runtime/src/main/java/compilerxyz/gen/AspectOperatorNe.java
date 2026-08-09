package compilerxyz.gen;


import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@BuiltinFull(uniqueId = 1059L, identityId = 4059L)
public interface AspectOperatorNe extends Aspect {
  EResult<CodeValue> aspectOperatorNeNe(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
