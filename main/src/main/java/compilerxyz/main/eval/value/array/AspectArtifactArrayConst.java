package compilerxyz.main.eval.value.array;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectArtifact;

public record AspectArtifactArrayConst<T extends Type>(TypeArrayConst<T> type) implements AspectArtifact {
    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return new ArtifactExporterArrayConst<>(type);
    }
}
