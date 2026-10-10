package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.eval.DefSetId;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public sealed interface ArtifactDataDefSetInterface extends SafeJsonSerializable {
    record Builtin(Unique identity) implements ArtifactDataDefSetInterface {}

    record User(
            Artifact defSet,
            DefSetId defSetId,
            Unique identity,
            Opt<String> description,
            boolean isAspect,
            Opt<Unique> aspectId,
            String binaryName,
            Opt<byte[]> compiledBytecode,
            TList<Artifact> methods)
            implements ArtifactDataDefSetInterface {}
}
