package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1031L, identityId = 4031L)
public interface AspectPostDeserialize extends Aspect {
  EResult<CodeValue> aspectPostDeserializePostDeserialize(
      ExprContext xx, QExprId loc, CodeValue cv);
}
