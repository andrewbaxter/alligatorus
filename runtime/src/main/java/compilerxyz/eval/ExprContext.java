package compilerxyz.eval;

import compilerxyz.eval.dynamic.Target;
import org.jspecify.annotations.Nullable;

public final class ExprContext {
  public final @Nullable Type currentExceptionType;
  public final ImportStateId importState;
  public final Scope scope;
  public final Target target;
  public final EvalContext vx;

  public ExprContext(
      Scope scope,
      Target target,
      EvalContext vx,
      ImportStateId importState,
      @Nullable Type currentExceptionType) {
    this.scope = scope;
    this.target = target;
    this.vx = vx;
    this.importState = importState;
    this.currentExceptionType = currentExceptionType;
  }

  public ExprContext child(Scope childScope) {
    return new ExprContext(childScope, target, vx, importState, currentExceptionType);
  }

  public ExprContext withExceptionType(@Nullable Type newExceptionType) {
    return new ExprContext(scope, target, vx, importState, newExceptionType);
  }
}
