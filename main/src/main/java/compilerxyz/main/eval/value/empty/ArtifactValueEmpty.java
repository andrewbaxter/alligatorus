package compilerxyz.main.eval.value.empty;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;

public record ArtifactValueEmpty(Artifact type) implements SafeJsonSerializable {}
