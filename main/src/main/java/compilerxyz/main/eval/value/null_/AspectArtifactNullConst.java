package compilerxyz.main.eval.value.null_;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactNullConst implements AspectArtifact {
    public static final AspectArtifactNullConst INSTANCE = new AspectArtifactNullConst();

    private AspectArtifactNullConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.NULL_CONST;
    }
}
