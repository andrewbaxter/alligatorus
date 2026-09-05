package compilerxyz.main.eval.value.record;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactRecord implements AspectArtifact {
    public static final AspectArtifactRecord INSTANCE = new AspectArtifactRecord();

    private AspectArtifactRecord() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.RECORD;
    }
}
