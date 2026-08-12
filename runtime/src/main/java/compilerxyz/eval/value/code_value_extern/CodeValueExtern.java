package compilerxyz.eval.value.code_value_extern;

import compilerxyz.eval.CodeValue;
import compilerxyz.util.Opt;

public final class CodeValueExtern {
  private Opt<CodeValue> value;

  public CodeValueExtern(CodeValue value) {
    this.value = Opt.some(value);
  }

  public CodeValue intoInner() {
    if (value.optIsNone()) {
      throw new IllegalStateException("CodeValueExtern: already taken");
    }
    var v = value.optUnwrap();
    value = Opt.none();
    return v;
  }
}
