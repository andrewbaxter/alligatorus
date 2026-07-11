package compilerxyz.eval.dynamic;

public final class DynException extends RuntimeException {
  public final Object payload;

  public DynException(Object payload) {
    super(payload == null ? "null" : payload.toString());
    this.payload = payload;
  }
}
