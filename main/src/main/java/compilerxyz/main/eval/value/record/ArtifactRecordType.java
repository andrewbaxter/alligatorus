package compilerxyz.main.eval.value.record;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.TOrderedMap;

public record ArtifactRecordType(TOrderedMap<ConstKey, Artifact> fields) implements SafeJsonSerializable {}
