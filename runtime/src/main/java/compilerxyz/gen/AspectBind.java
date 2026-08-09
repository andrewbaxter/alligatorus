package compilerxyz.gen;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;

@BuiltinFull(uniqueId = 1022L, identityId = 4022L)
@BuiltinPath("eval.aspect_bind")
public interface AspectBind extends Aspect {
  @BuiltinFull(uniqueId = 1044L)
  @BuiltinPath("types.bind_result")
  record BindResult(Code code, Binding binding) {
  }

  EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv);
}
