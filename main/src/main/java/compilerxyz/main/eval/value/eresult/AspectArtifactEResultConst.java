package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectArtifact;

public record AspectArtifactEResultConst<T extends Type>(TypeEResultConst<T> type) implements AspectArtifact {
    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return new ArtifactExporterEResultConst<>(type);
    }
}
