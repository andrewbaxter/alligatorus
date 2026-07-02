package compilerxyz.eval;

import compilerxyz.eval.value.constkey.TypeConstkeyDynamic;
import java.util.Comparator;

public sealed interface ConstKey {
  record Str(String value) implements ConstKey {}

  record Uint(long value) implements ConstKey {}

  Comparator<ConstKey> CANONICAL_ORDER = Comparator.comparing(ConstKey::toCanonical);

  default String toCanonical() {
    return switch (this) {
      case Str s -> "\"" + s.value().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
      case Uint u -> Long.toUnsignedString(u.value());
    };
  }

  static Type dynamicType() {
    return new TypeConstkeyDynamic();
  }
}
