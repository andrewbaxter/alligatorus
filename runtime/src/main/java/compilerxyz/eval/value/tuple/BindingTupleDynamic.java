package compilerxyz.eval.value.tuple;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.ValueDynamicSource;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record BindingTupleDynamic(BindingId dynamicKey, TList<Type> elements)
    implements Binding {

  @Override
  public CodeValue bindingUse(ExprContext xx, QExprId loc, Code code) {
    var resultElements = new TList<TupleElement>(elements.size());
    for (var t : elements) {
      resultElements.add(new TupleElement.Dynamic(t));
    }
    return new CodeValue(
        Code.of(loc, BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, dynamicKey)),
        EvalOutcome.ofValue(
            new Tuple(
                Opt.some(new TupleBaseDynamic(new ValueDynamicSource.Binding(dynamicKey))),
                resultElements)));
  }

  @Override
  public Binding bindingFork() {
    return this;
  }

  @Override
  public boolean bindingEquals(ExprContext xx, QExprId loc, Binding other) {
    return this == other || this.equals(other);
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
