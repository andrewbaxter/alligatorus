package compilerxyz.eval;

import compilerxyz.gen.Binding;
import compilerxyz.gen.BindingSetter;

/**
 * Root {@link BindingSetter} for a scope entry: replaces the entry's binding
 * in the live stack and advances its state ids. Closes over the entry object
 * itself, which is the fork copy live on the mutating path, so the
 * replacement lands on that path only.
 */
public final class ScopeEntrySetter implements BindingSetter {
  private final Scope scope;
  private final ScopeLevel.Entry entry;

  public ScopeEntrySetter(Scope scope, ScopeLevel.Entry entry) {
    this.scope = scope;
    this.entry = entry;
  }

  @Override
  public boolean bindingSetterSet(
      ExprContext xx, QExprId loc, Binding newBinding, boolean dropModified) {
    if (!scope.levels.contains(entry)) {
      return false;
    }
    entry.binding = newBinding;
    entry.state = scope.vx.dropStateCounter.next();
    if (dropModified) {
      entry.dropState = scope.vx.dropStateCounter.next();
    }
    return true;
  }
}
