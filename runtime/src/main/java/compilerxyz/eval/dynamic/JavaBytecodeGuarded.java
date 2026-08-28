package compilerxyz.eval.dynamic;

import compilerxyz.eval.JumpKey;

public class JavaBytecodeGuarded implements JavaBytecode {
    public final JumpKey dest;
    public final CodeDynamic inner;

    public JavaBytecodeGuarded(JumpKey dest, CodeDynamic inner) {
        this.dest = dest;
        this.inner = inner;
    }

    @Override
    public <T> T javaBytecodeDispatch(Dispatcher<T> dispatcher) {
        return dispatcher.dispatcherHandleGuarded(this);
    }
}
