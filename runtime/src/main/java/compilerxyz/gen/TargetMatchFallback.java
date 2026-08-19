package compilerxyz.gen;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.Type;

public record TargetMatchFallback(
    BindingId.Local payloadBinding, CodeValue body) {
  public static Type dynamicType() {
    return GenType.dynamicType();
  }
}
