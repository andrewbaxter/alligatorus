package compilerxyz.main.eval.value.source_union;

import compilerxyz.main.artifact.ArtifactSupport.ArtifactIdHolder;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.Type;

public final class SourceUnionVariant {
    public final ConstKey tag;
    public final String jsTag;
    public final Type type;
    public final ArtifactIdHolder newArtifactIdCell = new ArtifactIdHolder();

    public SourceUnionVariant(ConstKey tag, String jsTag, Type type) {
        this.tag = tag;
        this.jsTag = jsTag;
        this.type = type;
    }
}
