package compilerxyz.eval;

import compilerxyz.gen.Binding;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

/**
 * A stack of {@link Binding}s that constitute a lexical scope. Names live in a
 * parallel list owned by Scope — bindings themselves never carry a name.
 * Structural entries with no name (e.g., a foreach iterator whose only reason
 * to exist is stack cleanup on scope exit) push a synthetic unnamed Binding
 * alongside a {@code null} name.
 */
public final class Scope {
  public final TList<Binding> entries = new TList<>();
  public final TList<@Nullable ConstKey> names = new TList<>();

  public int depth() {
    return entries.size();
  }

  public void pushNamed(ConstKey name, Binding binding) {
    entries.add(binding);
    names.add(name);
  }

  public void push(Binding binding) {
    entries.add(binding);
    names.add(null);
  }

  public void pushAll(compilerxyz.util.TMap<ConstKey, Binding> named) {
    for (var entry : named.entrySet()) {
      pushNamed(entry.getKey(), entry.getValue());
    }
  }

  public void truncate(int targetDepth) {
    while (entries.size() > targetDepth) {
      entries.remove(entries.size() - 1);
      names.remove(names.size() - 1);
    }
  }

  public @Nullable Binding lookup(ConstKey name) {
    for (int i = names.size() - 1; i >= 0; i--) {
      var n = names.get(i);
      if (n != null && n.equals(name)) return entries.get(i);
    }
    return null;
  }
}
