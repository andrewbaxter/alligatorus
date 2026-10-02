package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactEResultConst implements AspectArtifact {
    public static final AspectArtifactEResultConst INSTANCE = new AspectArtifactEResultConst();

    private AspectArtifactEResultConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.ERESULT_CONST;
    }
}
