package compilerxyz.eval.dynamic;

public interface JavaBytecode {
  <T> T dispatch(Dispatcher<T> dispatcher);

  interface Dispatcher<T> {
    T handleInstruction(JavaBytecodeInstruction n);

    T handleSequence(JavaBytecodeSequence n);

    T handleStoreLoad(JavaBytecodeStoreLoad n);

    T handleLand(JavaBytecodeLand n);

    T handleJump(JavaBytecodeJump n);

    T handleGuarded(JavaBytecodeGuarded n);
  }

  interface DefaultDispatcher<T> extends Dispatcher<T> {
    default T handleDefault(JavaBytecode n) {
      return null;
    }

    @Override
    default T handleJump(JavaBytecodeJump n) {
      return handleDefault(n);
    }

    @Override
    default T handleGuarded(JavaBytecodeGuarded n) {
      return handleDefault(n);
    }

    @Override
    default T handleLand(JavaBytecodeLand n) {
      return handleDefault(n);
    }

    @Override
    default T handleInstruction(JavaBytecodeInstruction n) {
      return handleDefault(n);
    }

    @Override
    default T handleStoreLoad(JavaBytecodeStoreLoad n) {
      return handleDefault(n);
    }

    @Override
    default T handleSequence(JavaBytecodeSequence n) {
      return handleDefault(n);
    }
  }
}
