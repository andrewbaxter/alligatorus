package compilerxyz.main.eval.value.ulong;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactUlongConst implements AspectArtifact {
    public static final AspectArtifactUlongConst INSTANCE = new AspectArtifactUlongConst();

    private AspectArtifactUlongConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.ULONG_CONST;
    }
}
