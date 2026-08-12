package compilerxyz.eval.dynamic;

import compilerxyz.eval.JumpKey;

/**
 * A code unit whose exceptions route to {@code dest}. Point-shaped: it wraps
 * exactly the unit that can throw, so units never partially overlap — the
 * innermost enclosing guard of any instruction decides its handler, and the
 * render pass aggregates maximal contiguous same-dest runs into exception
 * table entries (catch-any; type dispatch is in-code at the terminal catch).
 */
public class JavaBytecodeGuarded implements JavaBytecode {
  public final JumpKey dest;
  public final CodeDynamic inner;

  public JavaBytecodeGuarded(JumpKey dest, CodeDynamic inner) {
    this.dest = dest;
    this.inner = inner;
  }

  @Override
  public <T> T javaBytecodeDispatch(Dispatcher<T> dispatcher) {
    return dispatcher.dispatcherHandleGuarded(this);
  }
}
