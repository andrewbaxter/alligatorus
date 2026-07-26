package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1036L, identityId = 4036L)
public interface AspectFieldUseConst extends Aspect {
  EResult<CodeValue> aspectFieldUseConstFieldUseConst(ExprContext xx, QExprId loc, CodeValue cv);
}
