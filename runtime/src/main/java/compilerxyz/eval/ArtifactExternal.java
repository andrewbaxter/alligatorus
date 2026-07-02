package compilerxyz.eval;

import compilerxyz.artifact.Artifact;
import compilerxyz.safejson.SafeJsonSerializable;

public sealed interface ArtifactExternal extends SafeJsonSerializable {

  long artifactExternalLocalId();

  record StagedExpr(long artifactExternalLocalId, ArtifactExpr expr) implements ArtifactExternal {}

  record StageLift(long artifactExternalLocalId, Artifact sourceType) implements ArtifactExternal {}

  record Type(long artifactExternalLocalId, Artifact typeArtifact) implements ArtifactExternal {}

  record Unique(long artifactExternalLocalId, compilerxyz.eval.Unique unique) implements ArtifactExternal {}

  record CapturedEntry(BindingId binding, ArtifactExternal external) {}
}
