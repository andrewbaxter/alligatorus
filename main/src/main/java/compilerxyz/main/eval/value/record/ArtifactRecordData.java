package compilerxyz.main.eval.value.record;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;

public record ArtifactRecordData(Artifact type, Artifact value) implements SafeJsonSerializable {}
