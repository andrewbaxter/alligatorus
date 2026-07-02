package compilerxyz.eval;

import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;

public sealed interface BindingId {
  record Builtin(long index) implements BindingId {}

  record Def(DefSetId defSet, long index) implements BindingId {}

  record Local(JavaBytecodeBindingKey key) implements BindingId {}
}
