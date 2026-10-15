package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.gen.AspectArtifact;

public record AspectArtifactTuple(Tuple type) implements AspectArtifact {
    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return new ArtifactExporterTuple(type);
    }
}
