package compilerxyz.eval.value.source_union_new;

import compilerxyz.eval.Type;
import compilerxyz.eval.Value;
import compilerxyz.eval.value.source_union.TypeSourceUnionDynamic;

public record SourceUnionNewRef(TypeSourceUnionDynamic data, int variantIndex) implements Value {
  @Override
  public Type valueTypeOf() {
    return new TypeSourceUnionNewDynamic();
  }
}
