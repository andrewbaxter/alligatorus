package compilerxyz.main.eval.value.map;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;

public record ArtifactMapType(Artifact keyType, Artifact valueType) implements SafeJsonSerializable {}
