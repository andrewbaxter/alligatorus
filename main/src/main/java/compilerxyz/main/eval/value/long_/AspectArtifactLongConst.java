package compilerxyz.main.eval.value.long_;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactLongConst implements AspectArtifact {
    public static final AspectArtifactLongConst INSTANCE = new AspectArtifactLongConst();

    private AspectArtifactLongConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.LONG_CONST;
    }
}
