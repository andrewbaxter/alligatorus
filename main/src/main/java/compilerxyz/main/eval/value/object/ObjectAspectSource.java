package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.Unique;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6405L)
public sealed interface ObjectAspectSource {

    record Static(Unique id, Unique identity) implements ObjectAspectSource {}

    record FromInterfaceData() implements ObjectAspectSource {}
}
