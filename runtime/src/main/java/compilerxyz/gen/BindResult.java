package compilerxyz.gen;


import compilerxyz.eval.BindingId;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.CodeDynamic;

public record BindResult(CodeDynamic code, BindingId.Local id) {
  public static Type dynamicType() {
    return compilerxyz.gen.GenType.dynamicType();
  }
}
