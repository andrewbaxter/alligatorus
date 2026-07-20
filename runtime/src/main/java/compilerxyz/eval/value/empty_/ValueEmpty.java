package compilerxyz.eval.value.empty_;

import compilerxyz.eval.Type;
import compilerxyz.gen.Value;

public record ValueEmpty(Type type) implements Value {
                public Type valueType() {
    return type;
  }
}
