package compilerxyz.eval.binding;

import compilerxyz.gen.Binding;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;

public record BindingValue(Value value) implements Binding {
  @Override
  public CodeValue bindingUse(ExprContext xx, QExprId loc, Code code) {
    return new CodeValue(Code.none(loc), Opt.some(value));
  }
}
