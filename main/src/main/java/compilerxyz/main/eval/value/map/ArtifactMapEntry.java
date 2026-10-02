package compilerxyz.main.eval.value.map;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;

public record ArtifactMapEntry(Artifact key, Artifact value) implements SafeJsonSerializable {}
