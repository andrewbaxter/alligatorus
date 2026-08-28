package compilerxyz.eval;

import compilerxyz.gen.Binding;
import compilerxyz.gen.BindingSetter;

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
