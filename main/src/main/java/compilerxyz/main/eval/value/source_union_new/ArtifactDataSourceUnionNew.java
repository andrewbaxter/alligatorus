package compilerxyz.main.eval.value.source_union_new;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;

public record ArtifactDataSourceUnionNew(Artifact union, int variantIndex) implements SafeJsonSerializable {}
