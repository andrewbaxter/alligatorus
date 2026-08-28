package compilerxyz.main.eval.dynamic;

import org.objectweb.asm.tree.AbstractInsnNode;

public class JavaBytecodeInstructionObj implements JavaBytecodeInstruction {

    public final AbstractInsnNode node;

    public JavaBytecodeInstructionObj(AbstractInsnNode node) {
        this.node = node;
    }

    @Override
    public void javaBytecodeInstructionDispatchMore(MoreDispatcher moreDispatcher) {
        moreDispatcher.moreDispatcherHandleObj(this);
    }

    @Override
    public <T> T javaBytecodeDispatch(JavaBytecode.Dispatcher<T> dispatcher) {
        return dispatcher.dispatcherHandleInstruction(this);
    }
}
