package compilerxyz.eval.dynamic.target;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.CodeDynamic;

@compilerxyz.annotations.BuiltinTypeId(7023L)
public record BindResult(CodeDynamic code, BindingId.Local id) {
  public static Type dynamicType() {
    return Type.dynamicType();
  }
}
