package compilerxyz.eval.binding;

import compilerxyz.gen.BindingSetter;



import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.Binding;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.eval.BindingId;

public record BindingObjectConst(Type type, Object data) implements Binding {
  @Override
  public CodeValue bindingUse(ExprContext xx, QExprId loc, Opt<BindingSetter> setter, Code code) {
    return new CodeValue(Code.none(loc), EvalOutcome.ofValue(new ValueObjectConst(type, data)));
  }

  @Override
  public Binding bindingFork() {
    return this;
  }



  @Override
  public EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others) {
    for (var other : others) {
      if (!this.equals(other)) {
        return EResult.err(loc,
            "cannot merge BindingObjectConst with a differently-typed binding across branches");
      }
    }
    return EResult.ok(this);
  }

  @Override
  public EResult<Code> bindingDrop(ExprContext xx, QExprId loc) {
    return Binding.bindingDrop(xx, loc, this);
  }

  @Override
  public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
    return Binding.elementUseDynamicBind(dynamicKey);
  }
}
