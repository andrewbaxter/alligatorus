package compilerxyz.main.eval.value.source_object_new;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactTypeSourceObjectNewConst implements AspectArtifact {
    public static final AspectArtifactTypeSourceObjectNewConst INSTANCE = new AspectArtifactTypeSourceObjectNewConst();

    private AspectArtifactTypeSourceObjectNewConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.SOURCE_OBJECT_NEW_CONST;
    }
}
