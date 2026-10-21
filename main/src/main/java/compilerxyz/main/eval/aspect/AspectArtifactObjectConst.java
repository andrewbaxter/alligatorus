package compilerxyz.main.eval.aspect;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.gen.AspectArtifact;

public record AspectArtifactObjectConst(ArtifactExporter exporter) implements AspectArtifact {
    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return exporter;
    }
}
