package compilerxyz.main.eval.value.double_;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactDoubleConst implements AspectArtifact {
    public static final AspectArtifactDoubleConst INSTANCE = new AspectArtifactDoubleConst();

    private AspectArtifactDoubleConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.DOUBLE_CONST;
    }
}
