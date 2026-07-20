package compilerxyz.gen;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@BuiltinAspect(
    uniqueId = 1031L,
    kind = "PostDeserialize",
    javadoc = "Called on imported values to convert them from const to dynamic form when appropriate.")
@compilerxyz.annotations.BuiltinTypeId(7229L)
public interface AspectPostDeserialize extends Aspect {
  EResult<CodeValue> aspectPostDeserializePostDeserialize(
      ExprContext xx, QExprId loc, CodeValue cv);
}
