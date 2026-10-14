package compilerxyz.main.eval.value;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;

public record ArtifactTypeOfType(Artifact type) implements SafeJsonSerializable {}
