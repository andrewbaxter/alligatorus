package compilerxyz.eval.dynamic;

public class JavaBytecodeCatch implements JavaBytecode {
  public final JavaBytecodeCatchKey key;
  public final CodeDynamic inner;

  public JavaBytecodeCatch(JavaBytecodeCatchKey key, CodeDynamic inner) {
    this.key = key;
    this.inner = inner;
  }

  @Override
  public <T> T dispatch(Dispatcher<T> dispatcher) {
    return dispatcher.handleCatch(this);
  }
}
