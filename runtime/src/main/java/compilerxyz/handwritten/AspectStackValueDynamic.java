package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.annotations.BuiltinAspectMethod;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;

@BuiltinAspect(
    uniqueId = 1025L,
    typeId = 11025L,
    name = "aspect_stack_value_dynamic",
    scopeId = 3011,
    fieldPrefix = "svd_",
    kind = "AspectStackValueDynamic")
public interface AspectStackValueDynamic extends Aspect {
  @BuiltinAspectMethod(name = "to_value")
  CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic self_);
}
