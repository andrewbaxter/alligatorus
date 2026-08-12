package compilerxyz.eval.value.numeric;

/** Unsigned-long conversions the JDK does not provide as a single builtin. */
public final class Unsigned {
  private Unsigned() {}

  @SuppressWarnings("unused")
  public static double toDouble(long x) {
    if (x >= 0) return (double) x;
    return ((double) (x >>> 1)) * 2.0 + (double) (x & 1L);
  }

  @SuppressWarnings("unused")
  public static float toFloat(long x) {
    if (x >= 0) return (float) x;
    return ((float) (x >>> 1)) * 2.0f + (float) (x & 1L);
  }
}
