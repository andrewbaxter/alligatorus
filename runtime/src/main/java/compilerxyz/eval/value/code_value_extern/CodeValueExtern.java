package compilerxyz.eval.value.code_value_extern;

import compilerxyz.eval.CodeValue;
import org.jspecify.annotations.Nullable;

public final class CodeValueExtern {
  private @Nullable CodeValue value;

  public CodeValueExtern(CodeValue value) {
    this.value = value;
  }

  public CodeValue intoInner() {
    var v = value;
    if (v == null) {
      throw new IllegalStateException("CodeValueExtern: already taken");
    }
    value = null;
    return v;
  }
}
