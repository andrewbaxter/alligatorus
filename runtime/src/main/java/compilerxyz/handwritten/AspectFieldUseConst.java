package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@BuiltinAspect(
    uniqueId = 1036L,
    kind = "FieldUseConst",
    javadoc = "Called on the type of a field's value to produce the CodeValue that a field expression evaluates to when the base is const.")
public interface AspectFieldUseConst extends Aspect {
  EResult<CodeValue> aspectFieldUseConstFieldUseConst(ExprContext xx, QExprId loc, CodeValue cv);
}
