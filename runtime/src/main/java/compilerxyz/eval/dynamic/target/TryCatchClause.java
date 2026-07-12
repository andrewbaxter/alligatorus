package compilerxyz.eval.dynamic.target;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaDataDescriptor;

public record TryCatchClause(
    BindingId.Local binding, JavaDataDescriptor payloadDesc, CodeDynamic body) {
  public static Type dynamicType() {
    return Type.dynamicType();
  }
}
