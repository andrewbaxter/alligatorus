package compilerxyz.main.eval.dynamic;

public final class DynPanic extends RuntimeException {
    public DynPanic(String message) {
        super(message);
    }
}
