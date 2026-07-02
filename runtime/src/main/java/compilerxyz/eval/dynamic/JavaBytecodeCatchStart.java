package compilerxyz.eval.dynamic;

public class JavaBytecodeCatchStart implements JavaBytecode {
  public final JavaBytecodeCatchKey key;

  public JavaBytecodeCatchStart(JavaBytecodeCatchKey key) {
    this.key = key;
  }

  @Override
  public <T> T dispatch(Dispatcher<T> dispatcher) {
  return dispatcher.handleCatchStart(this);
  }
}
