package compilerxyz.eval;

import compilerxyz.artifact.Artifact;
import compilerxyz.safejson.SafeJsonSerializable;

public sealed interface ArtifactExternal extends SafeJsonSerializable {
    record StagedExpr(ArtifactExpr expr) implements ArtifactExternal {}

    record StageLift(Artifact sourceTypeArtifact) implements ArtifactExternal {}

    record Type(Artifact typeArtifact) implements ArtifactExternal {}

    record UniqueRef(Unique unique) implements ArtifactExternal {}
}
