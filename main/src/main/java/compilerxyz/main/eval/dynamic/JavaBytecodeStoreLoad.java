package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.BindingId;

public class JavaBytecodeStoreLoad implements JavaBytecode {
    public final int code;
    public final BindingId key;

    public JavaBytecodeStoreLoad(int code, BindingId key) {
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
