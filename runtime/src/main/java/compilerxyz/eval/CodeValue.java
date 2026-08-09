package compilerxyz.eval;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.util.Opt;
import compilerxyz.gen.Value;

@BuiltinFull(uniqueId = 1041L)
@BuiltinPath("types.cve")
public record CodeValue(Code code, EvalOutcome outcome) {

  public Opt<Value> value() {
    if (outcome instanceof EvalOutcome.HasValue h) return Opt.some(h.value());
    return Opt.none();
  }

  public boolean controlEnded() {
    return outcome instanceof EvalOutcome.ControlEnded;
  }
}
