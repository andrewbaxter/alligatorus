package compilerxyz.prelude.util;

public final class Assert {
    private Assert() {}

    public static void notSame(Object a, Object b, String message) {
        if (a == b) throw new IllegalStateException(message);
    }
}
