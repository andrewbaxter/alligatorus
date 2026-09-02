package compilerxyz.main.eval.value.source_union;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;

public record ArtifactDataSourceUnionType(Artifact union) implements SafeJsonSerializable {}
