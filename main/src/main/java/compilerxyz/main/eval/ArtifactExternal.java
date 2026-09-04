package compilerxyz.main.eval;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;

public sealed interface ArtifactExternal extends SafeJsonSerializable {
    record StagedExpr(ArtifactExpr expr) implements ArtifactExternal {}

    record StageLift(Artifact sourceTypeArtifact) implements ArtifactExternal {}

    record Type(Artifact typeArtifact) implements ArtifactExternal {}

    record UniqueRef(Unique unique) implements ArtifactExternal {}

    record Loc(ArtifactExpr.ArtifactQExprId loc) implements ArtifactExternal {}
}
