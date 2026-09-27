package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public sealed interface ArtifactDataDefSetInterfaceMethod extends SafeJsonSerializable {
    record Builtin(Unique iface, String name) implements ArtifactDataDefSetInterfaceMethod {}

    record User(Artifact iface, String name, Artifact argType, Opt<Artifact> retType, TList<String> dataFieldNames)
            implements ArtifactDataDefSetInterfaceMethod {}
}
