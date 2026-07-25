package compilerxyz.eval.value.list;

import compilerxyz.artifact.Artifact;
import compilerxyz.safejson.SafeJsonSerializable;

public record ArtifactListType(Artifact elementType) implements SafeJsonSerializable {}
