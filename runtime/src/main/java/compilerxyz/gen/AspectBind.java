package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.annotations.BuiltinTypeId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1022L)
@BuiltinTypeId(7202L)
@BuiltinPath("eval.aspect_bind")
public interface AspectBind extends Aspect {
  @Builtin(uniqueId = 1044L)
  @BuiltinTypeId(7242L)
  @BuiltinPath("types.bind_result")
  record BindResult(Code code, Binding binding) {
    public static Type dynamicType() {
      return GenAspectBindBindResult.dynamicType();
    }
  }

  EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv);
}
