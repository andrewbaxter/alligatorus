package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.value.ValueDynamicSource;

@Builtin(uniqueId = 1039L, identityId = 4039L)
@BuiltinPath("eval.aspect_element_use_dynamic")
public interface AspectElementUseDynamic extends Aspect {
  EResult<CodeValue> aspectElementUseDynamicElementUseDynamic(
      ExprContext xx, QExprId loc, ValueDynamicSource elementSource, Code carry);
}
