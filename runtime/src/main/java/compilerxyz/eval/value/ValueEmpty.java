package compilerxyz.eval.value;

import compilerxyz.eval.Type;
import compilerxyz.gen.Value;

/**
 * A Value that carries only its Type — no ValueDynamic.Source, no wrapped
 * inner object. Used for compile-time-only values (builtins, function
 * references, and other "the semantics live entirely in the Type" cases)
 * where the runtime representation is inert.
 *
 * <p>Contrast with {@link ValueDynamic}, which additionally carries a
 * {@link ValueDynamic.Source} describing how to realize/drop the runtime
 * value.
 */
public record ValueEmpty(Type type_) implements Value {

  @Override
  public Type valueType() {
    return type_;
  }
}
