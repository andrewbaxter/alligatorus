package compilerxyz.eval.value.function;

import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Value;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.util.Opt;

public record BindingFunctionDynamic(BindingId dynamicKey, TypeFunctionDynamic type_)
    implements Binding {
  @Override
  public CodeValue bindingUse_(ExprContext xx, QExprId loc, Code code) {
    var empty = new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeSequence());
    Value value = new ValueDynamic(new ValueDynamic.Source.Binding(dynamicKey), type_);
    return new CodeValue(Code.of(loc, empty), Opt.some(value));
  }
}
