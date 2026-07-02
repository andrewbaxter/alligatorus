package compilerxyz.eval.value.source_union;

import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.Type;
import org.jspecify.annotations.Nullable;

public final class SourceUnionVariant {
  public final ConstKey tag;
  public final String jsTag;
  public final Type type;
  public @Nullable ArtifactIdSource newArtifactId;

  public SourceUnionVariant(ConstKey tag, String jsTag, Type type) {
    this.tag = tag;
    this.jsTag = jsTag;
    this.type = type;
  }
}
