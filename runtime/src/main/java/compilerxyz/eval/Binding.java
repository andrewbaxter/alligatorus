package compilerxyz.eval;

public interface Binding {

  default CodeValue bindingUse_(ExprContext xx, QExprId loc, Code code) {
    throw new AssertionError(
        "user did not provide a Binding::bindingUse_ method on " + getClass().getName());
  }
}
