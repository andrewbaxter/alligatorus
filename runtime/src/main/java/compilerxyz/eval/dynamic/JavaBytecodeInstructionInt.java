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
}
