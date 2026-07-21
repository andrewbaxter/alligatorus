package compilerxyz.eval.value.function;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.gen.Value;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record BindingFunctionDynamic(BindingId dynamicKey, TypeFunctionDynamic type_)
    implements Binding {
  @Override
  public CodeValue bindingUse(ExprContext xx, QExprId loc, Code code) {
    Value value = new ValueDynamic(new ValueDynamic.Source.Binding(dynamicKey), type_);
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
