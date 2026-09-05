package compilerxyz.main.eval.value.float_;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactFloatConst implements AspectArtifact {
    public static final AspectArtifactFloatConst INSTANCE = new AspectArtifactFloatConst();

    private AspectArtifactFloatConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.FLOAT_CONST;
    }
}
