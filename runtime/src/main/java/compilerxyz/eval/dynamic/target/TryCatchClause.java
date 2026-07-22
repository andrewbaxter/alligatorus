package compilerxyz.eval.dynamic.target;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.JavaDataDescriptor;

public record TryCatchClause(
    BindingId.Local binding, JavaDataDescriptor payloadDesc, CodeValue body) {
  public static Type dynamicType() {
    return compilerxyz.gen.GenType.dynamicType();
  }
}
