package compilerxyz.eval.value.interface_spec;

import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.eval.value.source_interface.SourceInterfaceRef;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record InterfaceSpec(
    JavaInternalName jvmInterfaceName,
    Unique identity,
    Opt<BuiltinInterfaceKind> builtinKind,
    TList<InterfaceMethodSpec> methods,
    Opt<SourceInterfaceRef> sourceRef)
    implements Value {

  @Override
  public Type valueTypeOf() {
    return new TypeInterfaceSpec();
  }
}
