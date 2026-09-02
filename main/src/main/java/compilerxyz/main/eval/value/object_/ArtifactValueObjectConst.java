package compilerxyz.main.eval.value.object_;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;

public record ArtifactValueObjectConst(Artifact type, Artifact data) implements SafeJsonSerializable {}
