package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.JumpKey;
import compilerxyz.prelude.util.Opt;

public class JavaBytecodeStackUnwind implements JavaBytecode {
    public final JumpKey level;
    public final Opt<JavaDataDescriptor> carry;

    public JavaBytecodeStackUnwind(JumpKey level, Opt<JavaDataDescriptor> carry) {
        this.level = level;
        this.carry = carry;
    }

    @Override
    public <T> T javaBytecodeDispatch(Dispatcher<T> dispatcher) {
        return dispatcher.dispatcherHandleStackUnwind(this);
    }
}
