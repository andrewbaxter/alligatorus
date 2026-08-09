package compilerxyz.gen;


import compilerxyz.annotations.BuiltinFull;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.value.ValueDynamicSource;

@BuiltinFull(uniqueId = 1037L, identityId = 4037L)
@BuiltinPath("eval.aspect_field_use_dynamic")
public interface AspectFieldUseDynamic extends Aspect {
  EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
      ExprContext xx, QExprId loc, ValueDynamicSource source);
}
