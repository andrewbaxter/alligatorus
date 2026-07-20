package compilerxyz.gen;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.value.ValueDynamic.Source;

@BuiltinAspect(
    uniqueId = 1039L,
    kind = "ElementUseDynamic",
    javadoc = "Called on the type of an element to produce the CodeValue that an element expression evaluates to when the container is dynamic.")
@compilerxyz.annotations.BuiltinTypeId(7212L)
public interface AspectElementUseDynamic extends Aspect {
  EResult<CodeValue> aspectElementUseDynamicElementUseDynamic(
      ExprContext xx, QExprId loc, Source elementSource, Code carry);
}
