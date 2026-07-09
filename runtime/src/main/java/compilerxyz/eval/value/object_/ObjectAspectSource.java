package compilerxyz.eval.value.object_;

import compilerxyz.eval.Unique;

public sealed interface ObjectAspectSource {

  record Static(Unique id) implements ObjectAspectSource {}

  record FromInterfaceData() implements ObjectAspectSource {}
}
