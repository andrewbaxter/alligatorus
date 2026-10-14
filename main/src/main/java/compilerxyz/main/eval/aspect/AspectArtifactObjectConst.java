package compilerxyz.main.eval.aspect;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactExporterHostObject;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactObjectConst implements AspectArtifact {
    public static final AspectArtifactObjectConst INSTANCE = new AspectArtifactObjectConst();

    private AspectArtifactObjectConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ArtifactExporterHostObject.INSTANCE;
    }
}
