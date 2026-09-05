package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.Unique;

public sealed interface ObjectAspectSource {

    record Static(Unique id, Unique identity) implements ObjectAspectSource {}

    record FromInterfaceData() implements ObjectAspectSource {}
}
