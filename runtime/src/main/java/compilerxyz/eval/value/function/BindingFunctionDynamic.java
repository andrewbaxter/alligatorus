package compilerxyz.eval.value.function;

import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.gen.Value;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.util.Opt;

public record BindingFunctionDynamic(BindingId dynamicKey, TypeFunctionDynamic type_)
    implements Binding {
  @Override
  public CodeValue bindingUse(ExprContext xx, QExprId loc, Code code) {
    Value value = new ValueDynamic(new ValueDynamic.Source.Binding(dynamicKey), type_);
    return new CodeValue(Code.none(loc), Opt.some(value));
  }
}
