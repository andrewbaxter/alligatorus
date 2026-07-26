package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1050L, identityId = 4050L)
public interface AspectOperatorSub extends Aspect {
  EResult<CodeValue> aspectOperatorSubSub(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
