package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;

public record ArtifactDataSourceObjectType(Artifact object) implements SafeJsonSerializable {}
