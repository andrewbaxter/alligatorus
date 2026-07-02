package compilerxyz.eval.value.source_union;

import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.eval.Counter;
import compilerxyz.eval.Unique;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

public final class SourceUnionData {
  public final Unique identity;
  public @Nullable ArtifactIdSource newArtifactId;
  public final Counter nextTagId;
  public final TList<SourceUnionVariant> variants;

  public SourceUnionData(Unique identity) {
    this.identity = identity;
    this.nextTagId = new Counter();
    this.variants = new TList<>();
  }
}
