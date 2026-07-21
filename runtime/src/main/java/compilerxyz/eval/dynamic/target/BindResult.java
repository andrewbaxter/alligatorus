package compilerxyz.eval.dynamic.target;

import compilerxyz.annotations.BuiltinTypeId;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.CodeDynamic;

@BuiltinTypeId(7023L)
public record BindResult(CodeDynamic code, BindingId.Local id) {
  public static Type dynamicType() {
    return compilerxyz.gen.GenType.dynamicType();
  }
}
