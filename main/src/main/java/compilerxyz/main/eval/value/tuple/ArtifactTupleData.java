package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;

public record ArtifactTupleData(Artifact type, Artifact value) implements SafeJsonSerializable {}
