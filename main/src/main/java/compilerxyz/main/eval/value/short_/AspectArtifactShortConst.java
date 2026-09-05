package compilerxyz.main.eval.value.short_;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactShortConst implements AspectArtifact {
    public static final AspectArtifactShortConst INSTANCE = new AspectArtifactShortConst();

    private AspectArtifactShortConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.SHORT_CONST;
    }
}
