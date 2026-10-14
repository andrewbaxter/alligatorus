package compilerxyz.main.eval.value.record;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.gen.AspectArtifact;

public record AspectArtifactRecord(Record type) implements AspectArtifact {
    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return new ArtifactExporterRecord(type);
    }
}
