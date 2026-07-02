package compilerxyz.eval;

public sealed interface BindingId {
  record Builtin(long index) implements BindingId {}

  record Def(DefSetId defSet, long index) implements BindingId {}

  record Local(long index) implements BindingId {}
}
