package compilerxyz.eval;

import compilerxyz.eval.aspect.Aspect;
import compilerxyz.util.Opt;

public interface Type extends Value {

  default String typeDesc() {
    throw new AssertionError(
        "user did not provide a Type::typeDesc method on " + getClass().getName());
  }

  default TypeId typeId() {
    throw new AssertionError(
        "user did not provide a Type::typeId method on " + getClass().getName());
  }

  default Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    return Opt.none();
  }

  default EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    throw new AssertionError(
        "Type::typeCastTo unreachable for " + getClass().getName()
            + " (host-only method; predicate typeCanCastTo returned true without an override)");
  }

  default boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return false;
  }

  default boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return false;
  }

  default EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new AssertionError(
        "user did not provide a Type::typeCastFrom method on " + getClass().getName());
  }
}
