package compilerxyz.main.eval.value.source_union;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.eval.ConstKey;

public record ArtifactDataSourceUnionVariant(String jsTag, ConstKey tag, Artifact type_) {}
