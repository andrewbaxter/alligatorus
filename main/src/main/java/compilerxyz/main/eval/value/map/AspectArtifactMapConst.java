package compilerxyz.main.eval.value.map;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactMapConst implements AspectArtifact {
    public static final AspectArtifactMapConst INSTANCE = new AspectArtifactMapConst();

    private AspectArtifactMapConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.MAP_CONST;
    }
}
