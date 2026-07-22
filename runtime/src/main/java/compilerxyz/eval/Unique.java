package compilerxyz.eval;

public sealed interface Unique {
  record Builtin(long id) implements Unique {}

  record Source(CompileId compileId, long id) implements Unique {}

  static Type dynamicType() {
    return new compilerxyz.eval.value.unique.TypeUniqueDynamic();
  }
}
