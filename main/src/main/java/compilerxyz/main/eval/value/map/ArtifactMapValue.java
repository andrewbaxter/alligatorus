package compilerxyz.main.eval.value.map;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.TList;

public record ArtifactMapValue(Artifact keyType, Artifact valueType, TList<ArtifactMapEntry> entries)
        implements SafeJsonSerializable {}
