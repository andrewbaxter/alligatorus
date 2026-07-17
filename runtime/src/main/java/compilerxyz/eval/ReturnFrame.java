package compilerxyz.eval;

import compilerxyz.eval.dynamic.JumpKey;
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;
import org.jspecify.annotations.Nullable;

public final class ReturnFrame {
  public final Opt<String> label;
  public final JumpKey endKey;
  public boolean wasTargeted = false;
  public @Nullable Value firstReturnValue;
  public int scopeDepthAtPush;

  public ReturnFrame(Opt<String> label, JumpKey endKey) {
    this.label = label;
    this.endKey = endKey;
  }
}
