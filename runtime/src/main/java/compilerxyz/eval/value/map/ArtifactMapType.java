package compilerxyz.eval.value.map;

import compilerxyz.artifact.Artifact;
import compilerxyz.safejson.SafeJsonSerializable;

public record ArtifactMapType(Artifact keyType, Artifact valueType) implements SafeJsonSerializable {}
