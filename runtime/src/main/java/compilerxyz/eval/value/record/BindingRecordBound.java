package compilerxyz.eval.value.record;

import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaBytecode;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TOrderedMap;

public record BindingRecordBound(
    TOrderedMap<ConstKey, RecordScopeField> fields, Opt<BindingId> jsKey) implements Binding {

  @Override
  public CodeValue bindingUse_(ExprContext xx, QExprId loc, Code code) {
    boolean hasDynamic = fields.values().stream().anyMatch(f -> f instanceof RecordScopeField.Dynamic);
    Code useCode;
    Opt<RecordBaseDynamic> base;
    if (hasDynamic) {
      if (!(jsKey instanceof Opt.Some<BindingId> some)) {
        throw new IllegalStateException(
            "BindingRecordBound: record has dynamic fields but no jsKey");
      }
      useCode = Code.of(loc, JavaBytecodeUtils.bindingUse(new CodeLoc.Some(loc), xx.vx, some.value()));
      base = Opt.some(new RecordBaseDynamic(new ValueDynamic.Source.Binding(some.value())));
    } else {
      useCode = Code.none(loc);
      base = Opt.none();
    }
    var resultFields = new TOrderedMap<ConstKey, RecordField>();
    for (var entry : fields.entrySet()) {
      RecordField rf =
          switch (entry.getValue()) {
            case RecordScopeField.Scope s -> new RecordField.EvalRes(
                EResult.ok(s.binding().bindingUse_(xx, loc, Code.none(loc))));
            case RecordScopeField.Dynamic d -> new RecordField.Dynamic(d.type());
            case RecordScopeField.Error e -> new RecordField.EvalRes(EResult.errs(e.errors()));
          };
      resultFields.put(entry.getKey(), rf);
    }
    return new CodeValue(useCode, Opt.some(new Record(base, resultFields)));
  }
}
