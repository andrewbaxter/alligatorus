package compilerxyz.eval.value.number;

public final class NumberFormat {
  private NumberFormat() {}

  public static String format(double d) {
    if (d == Math.floor(d) && !Double.isInfinite(d)) {
      return Long.toString((long) d);
    }
    return Double.toString(d);
  }
}
