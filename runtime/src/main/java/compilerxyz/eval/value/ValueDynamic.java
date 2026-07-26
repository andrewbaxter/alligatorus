package compilerxyz.eval.value;


import compilerxyz.annotations.BuiltinPath;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;

public record ValueDynamic(ValueDynamicSource source, Type type_) implements Value {
  public Type valueType() {
    return type_;
  }

  /**
   * Build a ValueDynamic whose Source is a fresh stack Expr sized to match the
   * type's JVM representation (2 slots for D/J primitives, 1 otherwise). Used
   * at every site that produces a value directly onto the JVM stack.
   */
  @BuiltinPath(value = "eval.stack", scopeId = 5013L)
  public static Value stack(Type type, long width) {
    return new ValueDynamic(new ValueDynamicSource.Expr((int) width), type);
  }
}
