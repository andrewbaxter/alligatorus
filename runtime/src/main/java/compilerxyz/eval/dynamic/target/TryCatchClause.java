package compilerxyz.eval.dynamic.target;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.CodeDynamic;

public record TryCatchClause(BindingId.Local binding, CodeDynamic body) {
  public static Type dynamicType() {
    return Type.dynamicType();
  }
}
