package compilerxyz.eval.value.source_union_new;

import compilerxyz.artifact.Artifact;
import compilerxyz.safejson.SafeJsonSerializable;

public record ArtifactDataSourceUnionNew(Artifact union, int variantIndex) implements SafeJsonSerializable {}
