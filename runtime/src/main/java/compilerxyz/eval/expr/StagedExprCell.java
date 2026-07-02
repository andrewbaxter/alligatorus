package compilerxyz.eval.expr;

import org.jspecify.annotations.Nullable;

public final class StagedExprCell {
  private @Nullable Object value;

  public StagedExprCell(Object value) {
    this.value = value;
  }

  public Object take() {
    var v = value;
    if (v == null) {
      throw new IllegalStateException("StagedExprCell already consumed");
    }
    value = null;
    return v;
  }

  public @Nullable Object peek() {
    return value;
  }
}
