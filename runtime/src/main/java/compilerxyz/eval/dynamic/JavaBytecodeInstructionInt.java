package compilerxyz.eval.dynamic;

public class JavaBytecodeInstructionInt implements JavaBytecodeInstruction {
    public final int code;

    public JavaBytecodeInstructionInt(int code) {
        this.code = code;
    }

    @Override
    public void javaBytecodeInstructionDispatchMore(MoreDispatcher moreDispatcher) {
        moreDispatcher.moreDispatcherHandleInt(this);
    }

    @Override
    public <T> T javaBytecodeDispatch(JavaBytecode.Dispatcher<T> dispatcher) {
        return dispatcher.dispatcherHandleInstruction(this);
    }
}
