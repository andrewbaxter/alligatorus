package compilerxyz.eval.value.binding;

import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Value;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaBytecode;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.util.Opt;

public record BindingDynamicBinding(BindingId dynamicKey, Type type_) implements Binding {
  @Override
  public CodeValue bindingUse_(ExprContext xx, QExprId loc, Code code) {
    CodeDynamic dyn = BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, dynamicKey);
    Value value = new ValueDynamic(new ValueDynamic.Source.Binding(dynamicKey), type_);
    return new CodeValue(Code.of(loc, dyn), Opt.some(value));
  }
}
