package compilerxyz.eval.value.binding;

import compilerxyz.gen.Binding;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;

public record BindingSourceObjectWrapper(Binding host, Binding underlying)
    implements Binding {
  @Override
  public CodeValue bindingUse(ExprContext xx, QExprId loc, Code code) {
    var underlyingCv = underlying.bindingUse(xx, loc, code);
    return host.bindingUse(xx, loc, underlyingCv.code());
  }
}
