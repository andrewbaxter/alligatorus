package compilerxyz.eval.binding;

import compilerxyz.eval.Binding;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.value.empty.ValueEmptyConst;
import compilerxyz.util.Opt;

public record BindingEmpty(Type type) implements Binding {
  @Override
  public CodeValue bindingUse_(ExprContext xx, QExprId loc, Code code) {
    return new CodeValue(Code.none(loc), Opt.some(new ValueEmptyConst(type)));
  }
}
