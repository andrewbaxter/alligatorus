package compilerxyz.main.eval.value.record;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;

public record ArtifactRecordField(boolean isValue, Artifact artifact) implements SafeJsonSerializable {}
