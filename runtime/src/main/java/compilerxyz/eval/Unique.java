package compilerxyz.eval;
import compilerxyz.eval.value.unique.TypeUniqueDynamic;

public sealed interface Unique {
  record Builtin(long id) implements Unique {}

  record Source(CompileId compileId, long id) implements Unique {}

  static Type dynamicType() {
    return new TypeUniqueDynamic();
  }
}
