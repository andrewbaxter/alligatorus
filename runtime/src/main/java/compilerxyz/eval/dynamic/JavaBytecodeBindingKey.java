package compilerxyz.eval.dynamic;

import compilerxyz.safejson.SafeJsonSerializable;
import java.util.concurrent.atomic.AtomicLong;

public record JavaBytecodeBindingKey(long id) implements SafeJsonSerializable {
  private static final AtomicLong COUNTER = new AtomicLong();

  public JavaBytecodeBindingKey() {
    this(COUNTER.getAndIncrement());
  }
}
