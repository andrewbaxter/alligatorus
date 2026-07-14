package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@BuiltinAspect(
    uniqueId = 1038L,
    kind = "ElementUseConst",
    javadoc = "Called on the type of an element to produce the CodeValue that an element expression evaluates to when the container is const.")
public interface AspectElementUseConst extends Aspect {
  EResult<CodeValue> aspectElementUseConstElementUseConst(
      ExprContext xx, QExprId loc, CodeValue cv);
}
