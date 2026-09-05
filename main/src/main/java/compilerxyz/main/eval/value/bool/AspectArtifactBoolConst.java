package compilerxyz.main.eval.value.bool;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactBoolConst implements AspectArtifact {
    public static final AspectArtifactBoolConst INSTANCE = new AspectArtifactBoolConst();

    private AspectArtifactBoolConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.BOOL_CONST;
    }
}
