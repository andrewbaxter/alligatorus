package compilerxyz.eval;

import compilerxyz.util.TList;

/**
 * Result of const-evaluating an expression: the produced {@link CodeValue},
 * the scope tree of the expression's default (fall-through) exit — installed
 * into the evaluation context before the next expression runs — and any
 * control-flow {@link Exit}s that escaped the expression and are still
 * looking for their merge point.
 */
public record EvalRes(CodeValue cv, Scope scope, TList<Exit> exits) {
  public static EvalRes of(CodeValue cv, Scope scope) {
    return new EvalRes(cv, scope, new TList<>());
  }
}
