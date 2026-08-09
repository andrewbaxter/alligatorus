package compilerxyz.eval.dynamic;

import compilerxyz.eval.JumpKey;


public class JavaBytecodeJump implements JavaBytecode {
    public final JumpKey jumpKey;

    public JavaBytecodeJump(JumpKey jumpKey) {
        this.jumpKey = jumpKey;
    }

    @Override
    public <T> T dispatch(Dispatcher<T> dispatcher) {
    return dispatcher.handleJump(this);
    }
}
