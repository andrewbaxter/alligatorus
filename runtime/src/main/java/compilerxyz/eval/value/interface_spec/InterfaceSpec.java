package compilerxyz.eval.value.interface_spec;

import compilerxyz.eval.Type;
import compilerxyz.eval.Value;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.util.TList;

public record InterfaceSpec(
    JavaInternalName jvmInterfaceName,
    TList<InterfaceMethodSpec> methods)
    implements Value {

  @Override
  public Type valueTypeOf() {
    return new TypeInterfaceSpec();
  }
}
