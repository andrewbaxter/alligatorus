package compilerxyz.eval;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.safejson.SafeJsonSerializable;

public sealed interface ArtifactExternal extends SafeJsonSerializable {
  JavaBytecodeBindingKey key();

  record StagedExpr(JavaBytecodeBindingKey key, ArtifactExpr expr) implements ArtifactExternal {}

  record StageLift(JavaBytecodeBindingKey key, Artifact sourceTypeArtifact)
      implements ArtifactExternal {}

  record Type(JavaBytecodeBindingKey key, Artifact typeArtifact) implements ArtifactExternal {}

  record Unique(JavaBytecodeBindingKey key, compilerxyz.eval.Unique unique)
      implements ArtifactExternal {}
}
