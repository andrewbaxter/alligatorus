package compilerxyz.eval.value;

import compilerxyz.eval.Type;
import compilerxyz.eval.Value;

/**
 * Opaque marker value with no per-instance data. Used for compile-time-only
 * values (like builtins, function references, and other "the semantics live
 * entirely in the Type" cases). The Type is carried alongside in the enclosing
 * CodeValue's binding metadata — callers must not rely on
 * ValueEmpty.valueTypeOf().
 */
public record ValueEmpty() implements Value {

  @Override
  public Type valueTypeOf() {
    throw new UnsupportedOperationException(
        "ValueEmpty::valueTypeOf: ValueEmpty is a marker; type lives on the binding/CodeValue,"
            + " not on the value itself");
  }
}
