package compilerxyz.eval;

import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.gen.GenType;

public sealed interface BindingId {
  static Type dynamicType() {
    return GenType.dynamicType();
  }

  record Local(JavaBytecodeBindingKey key) implements BindingId {}

    static Local localFresh() {
        return new Local(new JavaBytecodeBindingKey());
    }
}
