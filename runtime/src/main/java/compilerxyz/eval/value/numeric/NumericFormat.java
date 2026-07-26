package compilerxyz.eval.value.numeric;

public final class NumericFormat {
  private NumericFormat() {}

  public static String format(byte v) {
    return Byte.toString(v);
  }

  public static String format(short v) {
    return Short.toString(v);
  }

  public static String format(int v) {
    return Integer.toString(v);
  }

  public static String format(long v) {
    return Long.toString(v);
  }

  public static String format(float v) {
    return Float.toString(v);
  }

  public static String format(double v) {
    return Double.toString(v);
  }
}
