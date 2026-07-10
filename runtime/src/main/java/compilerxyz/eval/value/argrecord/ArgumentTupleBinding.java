package compilerxyz.eval.value.argrecord;

import compilerxyz.gen.Binding;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.util.Opt;

public record ArgumentTupleBinding(ArgumentTuple tuple) implements Binding {
  @Override
  public CodeValue bindingUse_(ExprContext xx, QExprId loc, Code code) {
    return new CodeValue(Code.none(loc), Opt.some(tuple));
  }
}
