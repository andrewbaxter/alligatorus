package compilerxyz.eval.dynamic;

import org.objectweb.asm.Label;

public class JavaBytecodeInstructionTryCatch implements JavaBytecodeInstruction {
    public final Label start;
    public final Label end;
    public final Label handler;
    public final String excType;

    public JavaBytecodeInstructionTryCatch(Label start, Label end, Label handler, String excType) {
        this.start = start;
        this.end = end;
        this.handler = handler;
        this.excType = excType;
    }

    @Override
    public void javaBytecodeInstructionDispatchMore(MoreDispatcher moreDispatcher) {
        moreDispatcher.moreDispatcherHandleTryCatch(this);
    }

    @Override
    public <T> T javaBytecodeDispatch(JavaBytecode.Dispatcher<T> dispatcher) {
        return dispatcher.dispatcherHandleInstruction(this);
    }
}
