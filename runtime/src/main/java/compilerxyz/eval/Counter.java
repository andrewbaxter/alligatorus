package compilerxyz.eval;

public final class Counter {
    private long n = 0;

    public long next() {
        var out = n;
        n = out + 1;
        return out;
    }
}
