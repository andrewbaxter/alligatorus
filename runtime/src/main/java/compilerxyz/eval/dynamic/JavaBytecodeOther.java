package compilerxyz.eval.dynamic;

import java.util.function.Supplier;

public class JavaBytecodeOther<Q> implements JavaBytecode{
    public final Supplier<Q> callback;

    public JavaBytecodeOther(Supplier<Q> callback) {
        this.callback = callback;
    }

    @Override
    public <T> T dispatch(Dispatcher<T> dispatcher) {
    // Not type safe... oh well
    return (T) callback.get();
    }
}
