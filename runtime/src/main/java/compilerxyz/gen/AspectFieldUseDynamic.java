package compilerxyz.gen;

import compilerxyz.annotations.BuiltinTypeId;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.value.ValueDynamicSource;

@Builtin(uniqueId = 1037L)
@BuiltinTypeId(7216L)
public interface AspectFieldUseDynamic extends Aspect {
  EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
      ExprContext xx, QExprId loc, ValueDynamicSource baseSource, String javaFieldName);
}
