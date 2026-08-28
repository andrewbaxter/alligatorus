package compilerxyz.eval.dynamic;

public interface JavaBytecode {
  <T> T javaBytecodeDispatch(Dispatcher<T> dispatcher);

  interface Dispatcher<T> {
    T dispatcherHandleInstruction(JavaBytecodeInstruction n);

    T dispatcherHandleSequence(JavaBytecodeSequence n);

    T dispatcherHandleStoreLoad(JavaBytecodeStoreLoad n);

    T dispatcherHandleLand(JavaBytecodeLand n);

    T dispatcherHandleJump(JavaBytecodeJump n);

    T dispatcherHandleGuarded(JavaBytecodeGuarded n);
  }
}
