package compilerxyz.main.eval.value.array;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;

public record ArtifactArrayType(Artifact elementType) implements SafeJsonSerializable {}
