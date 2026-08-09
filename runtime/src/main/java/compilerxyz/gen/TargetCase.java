package compilerxyz.gen;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.Type;

public record TargetCase(
    String dynamicTag,
    Type variantType,
    int variantIndex,
    BindingId.Local payloadBinding,
    CodeValue body) {
  public static Type dynamicType() {
    return compilerxyz.gen.GenType.dynamicType();
  }
}
