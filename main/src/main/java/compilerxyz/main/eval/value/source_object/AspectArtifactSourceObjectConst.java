package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactSourceObjectConst implements AspectArtifact {
    public static final AspectArtifactSourceObjectConst INSTANCE = new AspectArtifactSourceObjectConst();

    private AspectArtifactSourceObjectConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.SOURCE_OBJECT_VALUE_CONST;
    }
}
