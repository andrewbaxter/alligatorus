package compilerxyz.eval.value.record;

import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
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
import compilerxyz.util.TOrderedMap;

public record BindingRecordDynamic(BindingId dynamicKey, TOrderedMap<ConstKey, Type> fields)
    implements Binding {

  @Override
  public CodeValue bindingUse_(ExprContext xx, QExprId loc, Code code) {
    var resultFields = new TOrderedMap<ConstKey, RecordField>();
    for (var entry : fields.entrySet()) {
      resultFields.put(entry.getKey(), new RecordField.Dynamic(entry.getValue()));
    }
    return new CodeValue(
        Code.of(loc, BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, dynamicKey)),
        Opt.some(
            new Record(
                Opt.some(new RecordBaseDynamic(new ValueDynamic.Source.Binding(dynamicKey))),
                resultFields)));
  }
}
