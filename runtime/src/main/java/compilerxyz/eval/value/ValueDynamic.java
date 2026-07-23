package compilerxyz.eval.value;

import compilerxyz.annotations.BuiltinTypeId;

import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;

@BuiltinTypeId(7104L)
public record ValueDynamic(ValueDynamicSource source, Type type_) implements Value {
  public Type valueType() {
    return type_;
  }

  /**
   * Build a ValueDynamic whose Source is a fresh stack Expr sized to match the
   * type's JVM representation (2 slots for D/J primitives, 1 otherwise). Used
   * at every site that produces a value directly onto the JVM stack.
   */
  public static ValueDynamic stack(ExprContext xx, Type type_) {
    return new ValueDynamic(ValueDynamicSource.Expr.stackFor(xx, type_), type_);
  }
}
