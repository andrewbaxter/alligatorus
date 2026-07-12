package compilerxyz.eval.value.binding;

import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaBytecode;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.util.Opt;

public record BindingDynamicBinding(BindingId dynamicKey, Type type_) implements Binding {
  @Override
  public CodeValue bindingUse(ExprContext xx, QExprId loc, Code code) {
    CodeDynamic dyn;
    if (dynamicKey instanceof BindingId.Local local) {
      // Local slot: pick the typed load opcode from the binding's own type so a
      // primitive-typed slot yields a primitive on the stack directly.
      dyn = new CodeDynamic(
          new CodeLoc.Some(loc),
          new compilerxyz.eval.dynamic.JavaBytecodeStoreLoad(
              compilerxyz.eval.dynamic.JvmSort.loadOpcode(xx, type_), local.key()));
    } else {
      dyn = BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, dynamicKey);
    }
    Value value = new ValueDynamic(new ValueDynamic.Source.Binding(dynamicKey), type_);
    return new CodeValue(Code.of(loc, dyn), Opt.some(value));
  }
}
