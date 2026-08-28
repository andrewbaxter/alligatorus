package compilerxyz.eval.value.binding;

import compilerxyz.gen.BindingSetter;



import compilerxyz.eval.EvalOutcome;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.util.TList;
import compilerxyz.gen.Value;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaBytecode;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.ValueDynamicSource;
import compilerxyz.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.util.Opt;
import compilerxyz.eval.dynamic.JavaBytecodeStoreLoad;
import compilerxyz.eval.dynamic.JvmSort;

public record BindingDynamicBinding(BindingId dynamicKey, Type type_) implements Binding {
  @Override
  public CodeValue bindingUse(ExprContext xx, QExprId loc, Opt<BindingSetter> setter, Code code) {
    CodeDynamic dyn;
    dyn = switch (dynamicKey) {
      case BindingId.Local local ->
          new CodeDynamic(
              new CodeLoc.Some(loc),
              new JavaBytecodeStoreLoad(
                  JvmSort.loadOpcode(xx, type_), local.key()));
    };
    Value value = new ValueDynamic(new ValueDynamicSource.Binding(dynamicKey, setter), type_);
    return new CodeValue(Code.of(loc, dyn), EvalOutcome.ofValue(value));
  }

  @Override
  public Binding bindingFork() {
    return this;
  }



  @Override
  public EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others) {
    Type mergedType = type_;
    boolean merged = false;
    for (var other : others) {
      if (this.equals(other)) continue;
      if (!(other instanceof BindingDynamicBinding ob) || !dynamicKey.equals(ob.dynamicKey)) {
        return EResult.err(loc,
            "cannot merge " + getClass().getSimpleName()
                + " with a differently-typed binding across branches");
      }
      var aOpt = TypeSourceUnionDynamic.unionOf(xx, mergedType);
      var bOpt = TypeSourceUnionDynamic.unionOf(xx, ob.type_);
      if (aOpt.optIsNone()
          || bOpt.optIsNone()
          || !aOpt.optUnwrap().identity.equals(bOpt.optUnwrap().identity)) {
        return EResult.err(loc,
            "cannot merge " + getClass().getSimpleName()
                + " with a differently-typed binding across branches");
      }
      var mergedActive =
          new TList<Integer>(aOpt.optUnwrap().active.size() + bOpt.optUnwrap().active.size());
      mergedActive.addAll(aOpt.optUnwrap().active);
      mergedActive.addAll(bOpt.optUnwrap().active);
      mergedType = aOpt.optUnwrap().withActiveVariants(mergedActive);
      merged = true;
    }
    if (!merged) {
      return EResult.ok(this);
    }
    return EResult.ok(new BindingDynamicBinding(dynamicKey, mergedType));
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
