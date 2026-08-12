package compilerxyz.eval.dynamic;

import compilerxyz.eval.JumpKey;


public class JavaBytecodeJump implements JavaBytecode {
    public final JumpKey jumpKey;

    public JavaBytecodeJump(JumpKey jumpKey) {
        this.jumpKey = jumpKey;
    }

    @Override
    public <T> T javaBytecodeDispatch(Dispatcher<T> dispatcher) {
    return dispatcher.dispatcherHandleJump(this);
    }
}
