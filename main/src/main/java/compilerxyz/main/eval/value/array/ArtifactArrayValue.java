package compilerxyz.main.eval.value.array;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.TList;

public record ArtifactArrayValue(Artifact elementType, TList<Artifact> elements) implements SafeJsonSerializable {}
