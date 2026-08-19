package compilerxyz.eval.value.source_union;

import compilerxyz.util.Opt;
import compilerxyz.util.TList;

/**
 * Compiled-output state of a source union, assigned once at definition-set finish (or artifact
 * import). Shared by reference across all {@link TypeSourceUnionDynamic} instances of the union
 * so narrowed instances made before finish observe the assignment.
 */
public final class SourceUnionCompiled {
  public Opt<String> baseBinaryName = Opt.none();
  public Opt<TList<String>> variantBinaryNames = Opt.none();
  public Opt<byte[]> baseBytecode = Opt.none();
  public Opt<TList<byte[]>> variantBytecode = Opt.none();
}
