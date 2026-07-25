package compilerxyz.eval.value.option;

import compilerxyz.artifact.Artifact;
import compilerxyz.safejson.SafeJsonSerializable;

public record ArtifactOptionType(Artifact elementType) implements SafeJsonSerializable {}
