package compilerxyz.eval.value.binding;

import compilerxyz.eval.Binding;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;

public record BindingSourceObjectWrapper(compilerxyz.gen.Binding host, Binding underlying)
    implements Binding {
  @Override
  public CodeValue bindingUse_(ExprContext xx, QExprId loc, Code code) {
    var underlyingCv = underlying.bindingUse_(xx, loc, code);
    return host.bindingUse(xx, loc, underlyingCv.code());
  }
}
