package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Binding;

@BuiltinAspect(
    uniqueId = 1022L,
    kind = "Bind",
    javadoc = "Creates code to bind a value, and returns the binding (binding).")
public interface AspectBind extends Aspect {
  record BindResult(Code code, Binding binding) {}

  EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv);
}
