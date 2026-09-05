package compilerxyz.main.eval.value.string;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactStringConst implements AspectArtifact {
    public static final AspectArtifactStringConst INSTANCE = new AspectArtifactStringConst();

    private AspectArtifactStringConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.STRING_CONST;
    }
}
