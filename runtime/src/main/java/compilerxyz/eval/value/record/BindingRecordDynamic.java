package compilerxyz.eval.value.record;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaBytecode;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;

public record BindingRecordDynamic(BindingId dynamicKey, TOrderedMap<ConstKey, Type> fields)
    implements Binding {

  @Override
  public CodeValue bindingUse(ExprContext xx, QExprId loc, Code code) {
    var resultFields = new TOrderedMap<ConstKey, RecordField>();
    for (var entry : fields.entrySet()) {
      resultFields.put(entry.getKey(), new RecordField.Dynamic(entry.getValue()));
    }
    return new CodeValue(
        Code.of(loc, BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, dynamicKey)),
        EvalOutcome.ofValue(
            new Record(
                Opt.some(new RecordBaseDynamic(new ValueDynamic.Source.Binding(dynamicKey))),
                resultFields)));
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
