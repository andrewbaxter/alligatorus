package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.JumpKey;

public class JavaBytecodeLand implements JavaBytecode {
    public final JumpKey jumpKey;

    public JavaBytecodeLand(JumpKey jumpKey) {
        this.jumpKey = jumpKey;
    }

    @Override
    public <T> T javaBytecodeDispatch(Dispatcher<T> dispatcher) {
        return dispatcher.dispatcherHandleLand(this);
    }
}
