package compilerxyz.eval.dynamic;

public interface JavaBytecodeInstruction extends JavaBytecode {
    void javaBytecodeInstructionDispatchMore(MoreDispatcher moreDispatcher);

    public interface MoreDispatcher {
        void moreDispatcherHandleObj(JavaBytecodeInstructionObj n);

        void moreDispatcherHandleInt(JavaBytecodeInstructionInt n);

        void moreDispatcherHandleTryCatch(JavaBytecodeInstructionTryCatch n);
    }
}
