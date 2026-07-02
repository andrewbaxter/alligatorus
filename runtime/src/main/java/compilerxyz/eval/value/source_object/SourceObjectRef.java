package compilerxyz.eval.value.source_object;

import compilerxyz.eval.Type;
import compilerxyz.eval.Value;

public record SourceObjectRef(SourceObjectData data) implements Value {
  @Override
  public Type valueTypeOf() {
    return new TypeSourceObjectRefDynamic();
  }
}
