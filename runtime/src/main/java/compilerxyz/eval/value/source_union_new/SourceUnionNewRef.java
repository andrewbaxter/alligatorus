package compilerxyz.eval.value.source_union_new;

import compilerxyz.eval.Type;
import compilerxyz.eval.Value;
import compilerxyz.eval.value.source_union.SourceUnionData;

public record SourceUnionNewRef(SourceUnionData data, int variantIndex) implements Value {
  @Override
  public Type valueTypeOf() {
    return new TypeSourceUnionNewDynamic();
  }
}
