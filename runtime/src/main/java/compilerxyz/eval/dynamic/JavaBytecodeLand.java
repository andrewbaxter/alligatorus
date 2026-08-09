package compilerxyz.eval.dynamic;

import compilerxyz.eval.JumpKey;


public class JavaBytecodeLand implements JavaBytecode {
  public final JumpKey jumpKey;

  public JavaBytecodeLand(JumpKey jumpKey) {
    this.jumpKey = jumpKey;
  }

  @Override
  public <T> T dispatch(Dispatcher<T> dispatcher) {
    return dispatcher.handleLand(this);
  }
}
