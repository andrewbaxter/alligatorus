package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactTuple implements AspectArtifact {
    public static final AspectArtifactTuple INSTANCE = new AspectArtifactTuple();

    private AspectArtifactTuple() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.TUPLE;
    }
}
