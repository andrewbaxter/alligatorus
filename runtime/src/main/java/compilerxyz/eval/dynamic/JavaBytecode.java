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

  interface DefaultDispatcher<T> extends Dispatcher<T> {
    default T defaultDispatcherHandleDefault(JavaBytecode n) {
      return null;
    }

    @Override
    default T dispatcherHandleJump(JavaBytecodeJump n) {
      return defaultDispatcherHandleDefault(n);
    }

    @Override
    default T dispatcherHandleGuarded(JavaBytecodeGuarded n) {
      return defaultDispatcherHandleDefault(n);
    }

    @Override
    default T dispatcherHandleLand(JavaBytecodeLand n) {
      return defaultDispatcherHandleDefault(n);
    }

    @Override
    default T dispatcherHandleInstruction(JavaBytecodeInstruction n) {
      return defaultDispatcherHandleDefault(n);
    }

    @Override
    default T dispatcherHandleStoreLoad(JavaBytecodeStoreLoad n) {
      return defaultDispatcherHandleDefault(n);
    }

    @Override
    default T dispatcherHandleSequence(JavaBytecodeSequence n) {
      return defaultDispatcherHandleDefault(n);
    }
  }
}
