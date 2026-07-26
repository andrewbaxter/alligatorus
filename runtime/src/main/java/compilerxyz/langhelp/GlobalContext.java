package compilerxyz.langhelp;

import compilerxyz.artifact.Artifacts;
import compilerxyz.util.TOrderedMap;

public final class GlobalContext {
  public final Artifacts artifacts;
  public final Compiles compiles;
  public final Sources sources;
  public boolean captureBytecode;
  public final TOrderedMap<String, byte[]> capturedBytecode = new TOrderedMap<>();

  private GlobalContext(Sources sources, Artifacts artifacts, Compiles compiles) {
    this.sources = sources;
    this.artifacts = artifacts;
    this.compiles = compiles;
  }

  public static GlobalContext newContext(Sources sources, Artifacts artifacts, Compiles compiles) {
    return new GlobalContext(sources, artifacts, compiles);
  }
}
