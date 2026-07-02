package compilerxyz.eval.value.source_interface;

import compilerxyz.eval.Type;
import compilerxyz.eval.Value;

public record SourceInterfaceRef(SourceInterfaceData data) implements Value {
  @Override
  public Type valueTypeOf() {
    return new TypeSourceInterfaceDynamic();
  }
}
