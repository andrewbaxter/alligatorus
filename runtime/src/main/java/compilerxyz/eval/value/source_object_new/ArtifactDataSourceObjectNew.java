package compilerxyz.eval.value.source_object_new;

import compilerxyz.artifact.Artifact;
import compilerxyz.safejson.SafeJsonSerializable;

public record ArtifactDataSourceObjectNew(Artifact object) implements SafeJsonSerializable {}
