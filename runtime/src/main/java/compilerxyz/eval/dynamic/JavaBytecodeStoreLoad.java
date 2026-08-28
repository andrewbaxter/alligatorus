package compilerxyz.eval.dynamic;

public class JavaBytecodeStoreLoad implements JavaBytecode {
    public final int code;
    public final JavaBytecodeBindingKey key;

    public JavaBytecodeStoreLoad(int code, JavaBytecodeBindingKey key) {
        if (key == null) {
            throw new AssertionError();
        }
        this.code = code;
        this.key = key;
    }

    @Override
    public <T> T javaBytecodeDispatch(Dispatcher<T> dispatcher) {
        return dispatcher.dispatcherHandleStoreLoad(this);
    }
}
