package compilerxyz.eval;

import compilerxyz.eval.value.code_value_extern.TypeCodeValueExternDynamic;
import compilerxyz.util.Opt;
import compilerxyz.gen.Value;

public record CodeValue(Code code, EvalOutcome outcome) {

  public static Type dynamicType() {
    return new TypeCodeValueExternDynamic();
  }

  public Opt<Value> value() {
    if (outcome instanceof EvalOutcome.HasValue h) return Opt.some(h.value());
    return Opt.none();
  }

  public boolean controlEnded() {
    return outcome instanceof EvalOutcome.ControlEnded;
  }
}
