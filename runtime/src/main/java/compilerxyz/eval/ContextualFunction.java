package compilerxyz.eval;

@FunctionalInterface
public interface ContextualFunction {
  Object contextualFunctionApplyCtx(ExprContext xx, Object arg);
}
