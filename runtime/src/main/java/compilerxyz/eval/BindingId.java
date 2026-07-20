package compilerxyz.eval;

import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;

public sealed interface BindingId {
  static Type dynamicType() {
    return compilerxyz.gen.GenType.dynamicType();
  }

  record Local(JavaBytecodeBindingKey key) implements BindingId {}
}
