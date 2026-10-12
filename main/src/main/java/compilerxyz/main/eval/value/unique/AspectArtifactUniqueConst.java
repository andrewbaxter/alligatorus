package compilerxyz.main.eval.value.unique;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactUniqueConst implements AspectArtifact {
    public static final AspectArtifactUniqueConst INSTANCE = new AspectArtifactUniqueConst();

    private AspectArtifactUniqueConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.UNIQUE_CONST;
    }
}
