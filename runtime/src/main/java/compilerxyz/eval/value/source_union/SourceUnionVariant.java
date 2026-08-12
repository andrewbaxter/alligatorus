package compilerxyz.eval.value.source_union;

import compilerxyz.eval.ConstKey;
import compilerxyz.eval.Type;
import compilerxyz.artifact.ArtifactSupport.ArtifactIdHolder;

public final class SourceUnionVariant {
  public final ConstKey tag;
  public final String jsTag;
  public final Type type;
  public final ArtifactIdHolder newArtifactIdCell =
      new ArtifactIdHolder();

  public SourceUnionVariant(ConstKey tag, String jsTag, Type type) {
    this.tag = tag;
    this.jsTag = jsTag;
    this.type = type;
  }
}
