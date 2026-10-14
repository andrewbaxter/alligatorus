package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.gen.AspectArtifact;

public record AspectArtifactSourceObjectConst(TypeSourceObjectConst type) implements AspectArtifact {
    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return type.dynamic().instanceExporter();
    }
}
