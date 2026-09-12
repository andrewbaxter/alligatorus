package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.JumpKey;

public class JavaBytecodeStackMark implements JavaBytecode {
    public final JumpKey level;

    public JavaBytecodeStackMark(JumpKey level) {
        this.level = level;
    }

    @Override
    public <T> T javaBytecodeDispatch(Dispatcher<T> dispatcher) {
        return dispatcher.dispatcherHandleStackMark(this);
    }
}
