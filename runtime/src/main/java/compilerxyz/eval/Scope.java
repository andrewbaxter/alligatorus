package compilerxyz.eval;

import compilerxyz.util.TMap;
import org.jspecify.annotations.Nullable;

public final class Scope {
  public final @Nullable Scope parent;
  public final TMap<ConstKey, Binding> values = new TMap<>();

  public Scope(@Nullable Scope parent) {
    this.parent = parent;
  }
}
