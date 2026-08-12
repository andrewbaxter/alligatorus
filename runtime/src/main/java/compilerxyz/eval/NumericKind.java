package compilerxyz.eval;

/**
 * The JVM numeric primitive kinds. The unsigned kinds share the same JVM primitive (and bit pattern)
 * as their signed counterparts; only operations where signedness matters (division, remainder,
 * ordered comparison, right-shift, formatting and widening) differ. Shared by every numeric type
 * family and the {@code Numerics} operator/cast machinery.
 */
public enum NumericKind {
  BYTE,
  SHORT,
  INT,
  LONG,
  FLOAT,
  DOUBLE,
  UBYTE,
  USHORT,
  UINT,
  ULONG;

  public boolean integral() {
    return this != FLOAT && this != DOUBLE;
  }

  public boolean unsigned() {
    return this == UBYTE || this == USHORT || this == UINT || this == ULONG;
  }

  /** Two JVM stack slots (category-2) for long/double/ulong, one otherwise. */
  public boolean wide() {
    return this == LONG || this == DOUBLE || this == ULONG;
  }

  public long slots() {
    return wide() ? 2 : 1;
  }

  /** Integral bit width (8/16/32/64). Only defined for integral kinds. */
  public int intWidth() {
    return switch (this) {
      case BYTE, UBYTE -> 8;
      case SHORT, USHORT -> 16;
      case INT, UINT -> 32;
      case LONG, ULONG -> 64;
      case FLOAT, DOUBLE -> throw new IllegalStateException("intWidth on floating kind " + this);
    };
  }

  /** Unary numeric promotion: small kinds collapse to (u)int; wider kinds unchanged. */
  public static NumericKind unaryPromote(NumericKind a) {
    return switch (a) {
      case BYTE, SHORT, INT -> INT;
      case LONG -> LONG;
      case FLOAT -> FLOAT;
      case DOUBLE -> DOUBLE;
      case UBYTE, USHORT, UINT -> UINT;
      case ULONG -> ULONG;
    };
  }
}
