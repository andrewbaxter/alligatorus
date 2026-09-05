package compilerxyz.main.eval.value.int_;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactIntConst implements AspectArtifact {
    public static final AspectArtifactIntConst INSTANCE = new AspectArtifactIntConst();

    private AspectArtifactIntConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.INT_CONST;
    }
}
