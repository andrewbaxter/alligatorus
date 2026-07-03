package compilerxyz.eval;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.safejson.SafeJsonSerializable;

public record ArtifactExternal(
    JavaBytecodeBindingKey key, Artifact typeArtifact, Artifact valueArtifact)
    implements SafeJsonSerializable {}
