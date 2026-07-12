package compilerxyz.eval.value.source_union;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.ConstKey;

public record ArtifactDataSourceUnionVariant(String jsTag, ConstKey tag, Artifact type_) {}
