package compilerxyz.eval;

import compilerxyz.gen.Value;

/**
 * Three-state result of const-evaluating an expression:
 * <ul>
 *   <li>{@link ControlEnded} — the expression never returns normally (panic/throw/return).</li>
 *   <li>{@link NoValue} — the expression returns at runtime but has no compile-time-known value.</li>
 *   <li>{@link HasValue} — the expression returns with a compile-time-known value.</li>
 * </ul>
 * Replaces the earlier use of {@code Opt<Value>} where {@code None} conflated
 * "no compile-time value" with "control ended", causing spurious
 * "Unreachable" diagnostics when a runtime-valued expression's value was unknown at compile time.
 */
public sealed interface EvalOutcome {
  record ControlEnded() implements EvalOutcome {}

  record NoValue() implements EvalOutcome {}

  record HasValue(Value value) implements EvalOutcome {}

  ControlEnded CONTROL_ENDED = new ControlEnded();
  NoValue NO_VALUE = new NoValue();

  static EvalOutcome ofValue(Value v) {
    return new HasValue(v);
  }
}
