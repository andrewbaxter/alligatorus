package compilerxyz.eval;

import compilerxyz.util.TList;

/**
 * Result of const-evaluating an expression: the produced {@link CodeValue}
 * plus any control-flow {@link Exit}s that escaped the expression and are
 * still looking for their merge point.
 */
public record EvalRes(CodeValue cv, TList<Exit> exits) {
  public static EvalRes of(CodeValue cv) {
    return new EvalRes(cv, new TList<>());
  }
}
