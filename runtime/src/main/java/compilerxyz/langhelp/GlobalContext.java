package compilerxyz.langhelp;

import compilerxyz.artifact.Artifacts;
import compilerxyz.eval.CompileId;
import compilerxyz.eval.CompileRes;
import compilerxyz.util.TMap;
import compilerxyz.util.TOrderedMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jspecify.annotations.Nullable;

public final class GlobalContext {
  public final Artifacts artifacts;
  public final TMap<CompileId, CompileResultEntry> compileResults = new TMap<>();
  public final ReentrantLock compileResultsLock = new ReentrantLock();
  public final Sources sources;
  public boolean captureBytecode;
  public final TOrderedMap<String, byte[]> capturedBytecode = new TOrderedMap<>();

  private GlobalContext(Sources sources, Artifacts artifacts) {
    this.sources = sources;
    this.artifacts = artifacts;
  }

  public static GlobalContext newContext(Sources sources, Artifacts artifacts) {
    return new GlobalContext(sources, artifacts);
  }

  public static final class CompileResultEntry {
    public final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    public @Nullable CompileRes value;
  }
}
