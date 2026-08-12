package compilerxyz.eval;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.safejson.SafeJsonSerializable;

public sealed interface ArtifactExternal extends SafeJsonSerializable {
  JavaBytecodeBindingKey artifactExternalKey();

  record StagedExpr(JavaBytecodeBindingKey artifactExternalKey, ArtifactExpr expr) implements ArtifactExternal {}

  record StageLift(JavaBytecodeBindingKey artifactExternalKey, Artifact sourceTypeArtifact)
      implements ArtifactExternal {}

  record Type(JavaBytecodeBindingKey artifactExternalKey, Artifact typeArtifact) implements ArtifactExternal {}

  record UniqueRef(JavaBytecodeBindingKey artifactExternalKey, Unique unique)
      implements ArtifactExternal {}
}
