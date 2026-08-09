package compilerxyz.eval;

/**
 * One expression's evaluation result: the code+value pair and the
 * default-exit scope tree, installed into the context before the next
 * expression evaluates. Control exits register directly on their target
 * scope levels; nothing else passes upward.
 */
public record EvalRes(CodeValue cv, Scope scope) {
  public static EvalRes of(CodeValue cv, Scope scope) {
    return new EvalRes(cv, scope);
  }
}
