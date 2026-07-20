package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.value.bind_result.BindResultType;
import compilerxyz.gen.Binding;

@Builtin(uniqueId = 1022L)
@compilerxyz.annotations.BuiltinTypeId(7202L)
public interface AspectBind extends Aspect {
  record BindResult(Code code, Binding binding) {
    public static Type dynamicType() {
      return BindResultType.dynamicType();
    }
  }

  EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv);
}
