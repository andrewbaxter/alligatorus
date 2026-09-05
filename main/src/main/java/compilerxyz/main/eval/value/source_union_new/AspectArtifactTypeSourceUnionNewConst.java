package compilerxyz.main.eval.value.source_union_new;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactTypeSourceUnionNewConst implements AspectArtifact {
    public static final AspectArtifactTypeSourceUnionNewConst INSTANCE = new AspectArtifactTypeSourceUnionNewConst();

    private AspectArtifactTypeSourceUnionNewConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.SOURCE_UNION_NEW_CONST;
    }
}
