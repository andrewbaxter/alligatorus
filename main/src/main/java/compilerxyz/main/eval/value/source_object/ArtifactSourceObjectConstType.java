package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;

public record ArtifactSourceObjectConstType(Artifact dynamic) implements SafeJsonSerializable {}
