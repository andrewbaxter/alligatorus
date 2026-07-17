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

  /**
   * Clone the scope for a branch — every binding is forked so subsequent
   * mutations in the branch do not leak into the parent scope or into
   * sibling branches. Names are copied verbatim (the name→position
   * mapping is scope metadata, not per-binding state).
   */
  public Scope fork() {
    var out = new Scope();
    for (int i = 0; i < entries.size(); i++) {
      out.entries.add(entries.get(i).bindingFork());
      out.names.add(names.get(i));
    }
    return out;
  }

  /**
   * Merge branch outcomes into this scope. Each entry is merged position-wise
   * with the corresponding entry in {@code others}. If any per-position
   * merge fails (e.g., mutually-exclusive type states across branches),
   * returns the error without altering this scope's state.
   */
  public EResult<Void> merge(ExprContext xx, QExprId loc, TList<Scope> others) {
    for (var other : others) {
      if (other.entries.size() != entries.size()) {
        return EResult.err(loc,
            "Scope.merge: branch produced a scope of different depth ("
                + other.entries.size() + " vs " + entries.size() + ")");
      }
    }
    var merged = new TList<Binding>(entries.size());
    for (int i = 0; i < entries.size(); i++) {
      var branchBindings = new TList<Binding>(others.size());
      for (var other : others) branchBindings.add(other.entries.get(i));
      var mergedRes = entries.get(i).bindingMerge(xx, loc, branchBindings);
      if (mergedRes.hasErrors()) return mergedRes.reerr();
      merged.add(mergedRes.requireValue("Scope.merge binding"));
    }
    for (int i = 0; i < entries.size(); i++) entries.set(i, merged.get(i));
    return EResult.ok(null);
  }

  /**
   * Replace this scope's contents with {@code other}'s. Used to install a
   * fork before running a branch, and to install the merge result after.
   */
  public void replace(Scope other) {
    entries.clear();
    names.clear();
    for (int i = 0; i < other.entries.size(); i++) {
      entries.add(other.entries.get(i));
      names.add(other.names.get(i));
    }
  }
}
