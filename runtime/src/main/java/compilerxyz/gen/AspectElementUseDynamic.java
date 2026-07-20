package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.value.ValueDynamic.Source;

@Builtin(uniqueId = 1039L)
@compilerxyz.annotations.BuiltinTypeId(7212L)
public interface AspectElementUseDynamic extends Aspect {
  EResult<CodeValue> aspectElementUseDynamicElementUseDynamic(
      ExprContext xx, QExprId loc, Source elementSource, Code carry);
}
