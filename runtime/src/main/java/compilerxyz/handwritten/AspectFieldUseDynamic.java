package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.value.ValueDynamic.Source;

@BuiltinAspect(
    uniqueId = 1037L,
    kind = "FieldUseDynamic",
    javadoc = "Called on the type of a field's value to produce the CodeValue that a field expression evaluates to when the base is dynamic.")
public interface AspectFieldUseDynamic extends Aspect {
  EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
      ExprContext xx, QExprId loc, Source baseSource, String javaFieldName);
}
