package compilerxyz.eval;

import compilerxyz.annotations.Builtin;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.util.Opt;
import compilerxyz.gen.Value;

@Builtin(uniqueId = 1041L)
@BuiltinPath("types.cve")
public record CodeValue(Code code, EvalOutcome outcome) {

  public static Type dynamicType() {
    return compilerxyz.gen.GenCodeValue.dynamicType();
  }

  public Opt<Value> value() {
    if (outcome instanceof EvalOutcome.HasValue h) return Opt.some(h.value());
    return Opt.none();
  }

  public boolean controlEnded() {
    return outcome instanceof EvalOutcome.ControlEnded;
  }
}
