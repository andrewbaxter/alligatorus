package compilerxyz.eval;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.safejson.SafeJsonSerializable;

public sealed interface ArtifactExternal extends SafeJsonSerializable {

  JavaBytecodeBindingKey artifactExternalLocalId();

  record StagedExpr(JavaBytecodeBindingKey artifactExternalLocalId, ArtifactExpr expr) implements ArtifactExternal {}

  record StageLift(JavaBytecodeBindingKey artifactExternalLocalId, Artifact sourceType) implements ArtifactExternal {}

  record Type(JavaBytecodeBindingKey artifactExternalLocalId, Artifact typeArtifact) implements ArtifactExternal {}

  record Unique(JavaBytecodeBindingKey artifactExternalLocalId, compilerxyz.eval.Unique unique) implements ArtifactExternal {}

  record CapturedEntry(BindingId binding, ArtifactExternal external) {}
}
