package compilerxyz.eval.value.object_;

import compilerxyz.eval.Unique;

public sealed interface ObjectAspectSource {

  record Static(Unique id, Unique identity) implements ObjectAspectSource {}

  record FromInterfaceData() implements ObjectAspectSource {}
}
