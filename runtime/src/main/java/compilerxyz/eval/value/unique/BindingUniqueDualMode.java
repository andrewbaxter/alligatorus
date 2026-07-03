package compilerxyz.eval.value.unique;

import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Unique;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaBytecode;
import compilerxyz.util.Opt;

public record BindingUniqueDualMode(BindingId dynamicKey, Unique unique) implements Binding {
  @Override
  public CodeValue bindingUse_(ExprContext xx, QExprId loc, Code code) {
    var dyn = JavaBytecodeUtils.bindingUse(new CodeLoc.Some(loc), xx.vx, dynamicKey);
    return new CodeValue(Code.of(loc, dyn), Opt.some(new ValueUnique(unique)));
  }
}
