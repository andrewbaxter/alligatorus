package compilerxyz.eval;

import compilerxyz.eval.value.any.TypeAnyDynamic;

public interface Value {

  default Type valueTypeOf() {
    throw new AssertionError(
        "user did not provide a Value::valueTypeOf method on " + getClass().getName());
  }

  static Type dynamicType() {
    return new TypeAnyDynamic();
  }
}
