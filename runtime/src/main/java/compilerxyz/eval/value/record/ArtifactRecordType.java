package compilerxyz.eval.value.record;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.ConstKey;
import compilerxyz.util.TOrderedMap;

public record ArtifactRecordType(TOrderedMap<ConstKey, Artifact> fields) {}
