package compilerxyz.util;

public final class MustCast {
  private MustCast() {}

  public static <T> T mustCast(Class<T> cls, Object o) {
    return cls.cast(o);
  }
}
