package compilerxyz.main.eval.value.option;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectArtifact;

public record AspectArtifactOptionConst<T extends Type>(TypeOptionConst<T> type) implements AspectArtifact {
    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return new ArtifactExporterOptionConst<>(type);
    }
}
