package compilerxyz.eval;

import compilerxyz.eval.dynamic.JumpKey;
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;

/**
 * A control-flow branch that exited its expression via a (possibly labeled)
 * return and has not yet been connected to its merge point. Carried upward in
 * {@link EvalRes}; each enclosing control construct either consumes it (lands
 * it at the construct's end) or forwards it after dropping the construct's own
 * scope levels through a trampoline.
 */
public final class Exit {
  public final QExprId loc;
  /** Target label; none targets the nearest enclosing control construct. */
  public final Opt<String> label;
  /** Jump emitted at the exit site; the consumer or trampoline lands it. */
  public final JumpKey key;
  /** Value carried by the exit (implicit empty tuple when the return had none). */
  public final Value value;
  /** True when the value's dynamic representation rides the operand stack across the jump. */
  public final boolean stackCarried;
  /**
   * Fork of the scope tree at the exit site. As the exit passes out of a
   * construct, the construct drops its levels from this fork (per-fork drop
   * behavior) and truncates them; at consumption the remainder is merged with
   * the fall-through scope.
   */
  public final Scope scope;

  public Exit(
      QExprId loc, Opt<String> label, JumpKey key, Value value, boolean stackCarried, Scope scope) {
    this.loc = loc;
    this.label = label;
    this.key = key;
    this.value = value;
    this.stackCarried = stackCarried;
    this.scope = scope;
  }
}
