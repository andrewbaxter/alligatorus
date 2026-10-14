package compilerxyz.main.eval.value.map;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectArtifact;

public record AspectArtifactMapConst<K extends Type, V extends Type>(TypeMapConst<K, V> type)
        implements AspectArtifact {
    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return new ArtifactExporterMapConst<>(type);
    }
}
