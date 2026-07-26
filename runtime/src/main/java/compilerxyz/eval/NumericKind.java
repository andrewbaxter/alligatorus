package compilerxyz.eval;

/**
 * The JVM numeric primitive kinds, in widening rank order (ordinal = rank). Shared by every numeric
 * type family and the {@code Numerics} operator/cast machinery.
 */
public enum NumericKind {
  BYTE,
  SHORT,
  INT,
  LONG,
  FLOAT,
  DOUBLE;

  public boolean integral() {
    return this == BYTE || this == SHORT || this == INT || this == LONG;
  }

  /** Two JVM stack slots (category-2) for long/double, one otherwise. */
  public boolean wide() {
    return this == LONG || this == DOUBLE;
  }

  public long slots() {
    return wide() ? 2 : 1;
  }

  /** Java binary numeric promotion: byte/short/int collapse to int. */
  public static NumericKind promote(NumericKind a, NumericKind b) {
    if (a == DOUBLE || b == DOUBLE) return DOUBLE;
    if (a == FLOAT || b == FLOAT) return FLOAT;
    if (a == LONG || b == LONG) return LONG;
    return INT;
  }

  /** Java unary numeric promotion: byte/short/int collapse to int; long/float/double unchanged. */
  public static NumericKind unaryPromote(NumericKind a) {
    return switch (a) {
      case BYTE, SHORT, INT -> INT;
      case LONG -> LONG;
      case FLOAT -> FLOAT;
      case DOUBLE -> DOUBLE;
    };
  }
}
