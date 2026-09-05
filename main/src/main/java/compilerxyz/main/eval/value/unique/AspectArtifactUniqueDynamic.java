package compilerxyz.main.eval.value.unique;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactUniqueDynamic implements AspectArtifact {
    public static final AspectArtifactUniqueDynamic INSTANCE = new AspectArtifactUniqueDynamic();

    private AspectArtifactUniqueDynamic() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.UNIQUE;
    }
}
