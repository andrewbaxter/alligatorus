package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1038L, identityId = 4038L)
public interface AspectElementUseConst extends Aspect {
  EResult<CodeValue> aspectElementUseConstElementUseConst(
      ExprContext xx, QExprId loc, CodeValue cv);
}
