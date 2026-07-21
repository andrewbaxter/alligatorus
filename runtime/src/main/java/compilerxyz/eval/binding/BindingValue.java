package compilerxyz.eval.binding;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.gen.Binding;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record BindingValue(Value value) implements Binding {
  @Override
  public CodeValue bindingUse(ExprContext xx, QExprId loc, Code code) {
    return new CodeValue(Code.none(loc), EvalOutcome.ofValue(value));
  }

  @Override
  public Binding bindingFork() {
    return this;
  }

  @Override
  public EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others) {
    for (var other : others) {
      if (other != this && !this.equals(other)) {
        return EResult.err(loc,
            "cannot merge " + getClass().getSimpleName()
                + " with a differently-typed binding across branches");
      }
    }
    return EResult.ok(this);
  }
}
