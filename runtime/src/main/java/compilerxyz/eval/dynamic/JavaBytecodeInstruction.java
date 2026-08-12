package compilerxyz.eval.dynamic;

public interface JavaBytecodeInstruction extends JavaBytecode {
  default <T> T javaBytecodeDispatch(JavaBytecode.Dispatcher<T> dispatcher) {
    return dispatcher.dispatcherHandleInstruction(this);
  }

  void javaBytecodeInstructionDispatchMore(MoreDispatcher moreDispatcher);

  public interface MoreDispatcher {
    void moreDispatcherHandleObj(JavaBytecodeInstructionObj n);

    void moreDispatcherHandleInt(JavaBytecodeInstructionInt n);

    void moreDispatcherHandleTryCatch(JavaBytecodeInstructionTryCatch n);
  }
}
