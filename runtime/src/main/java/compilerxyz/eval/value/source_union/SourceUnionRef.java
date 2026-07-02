package compilerxyz.eval.value.source_union;

import compilerxyz.eval.Type;
import compilerxyz.eval.Value;

public record SourceUnionRef(SourceUnionData data) implements Value {
  @Override
  public Type valueTypeOf() {
    return new TypeSourceUnionRefDynamic();
  }
}
