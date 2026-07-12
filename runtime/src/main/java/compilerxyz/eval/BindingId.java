package compilerxyz.eval;

import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;

public sealed interface BindingId {
  static Type dynamicType() {
    return Type.dynamicType();
  }

  record Builtin(long index) implements BindingId {}

  record Local(JavaBytecodeBindingKey key) implements BindingId {}
}
