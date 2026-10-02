package compilerxyz.main.eval.value.array;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactArrayConst implements AspectArtifact {
    public static final AspectArtifactArrayConst INSTANCE = new AspectArtifactArrayConst();

    private AspectArtifactArrayConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.ARRAY_CONST;
    }
}
