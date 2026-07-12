package compilerxyz.eval.value.source_union;

import compilerxyz.eval.ConstKey;
import compilerxyz.eval.Type;

public final class SourceUnionVariant {
  public final ConstKey tag;
  public final String jsTag;
  public final Type type;
  public final compilerxyz.artifact.ArtifactSupport.ArtifactIdHolder newArtifactIdCell =
      new compilerxyz.artifact.ArtifactSupport.ArtifactIdHolder();

  public SourceUnionVariant(ConstKey tag, String jsTag, Type type) {
    this.tag = tag;
    this.jsTag = jsTag;
    this.type = type;
  }
}
