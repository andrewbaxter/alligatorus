package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1064L, identityId = 4064L)
public interface AspectOperatorBitNot extends Aspect {
  EResult<CodeValue> aspectOperatorBitNotBitNot(ExprContext xx, QExprId loc, CodeValue base);
}
