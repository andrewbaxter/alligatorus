package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1055L, identityId = 4055L)
public interface AspectOperatorXor extends Aspect {
  EResult<CodeValue> aspectOperatorXorXor(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
