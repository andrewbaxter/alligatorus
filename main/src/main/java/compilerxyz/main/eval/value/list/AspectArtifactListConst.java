package compilerxyz.main.eval.value.list;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectArtifact;

public record AspectArtifactListConst<T extends Type>(TypeListConst<T> type) implements AspectArtifact {
    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return new ArtifactExporterListConst<>(type);
    }
}
