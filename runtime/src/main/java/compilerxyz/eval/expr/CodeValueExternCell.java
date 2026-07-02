package compilerxyz.eval.expr;

import compilerxyz.eval.CodeValue;
import org.jspecify.annotations.Nullable;

public final class CodeValueExternCell {
  private @Nullable CodeValue value;

  public CodeValueExternCell(CodeValue value) {
    this.value = value;
  }

  public CodeValue take() {
    var v = value;
    if (v == null) {
      throw new IllegalStateException("CodeValueExternCell already consumed");
    }
    value = null;
    return v;
  }

  public @Nullable CodeValue peek() {
    return value;
  }
}
