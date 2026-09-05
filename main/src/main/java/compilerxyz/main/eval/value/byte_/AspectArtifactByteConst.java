package compilerxyz.main.eval.value.byte_;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactByteConst implements AspectArtifact {
    public static final AspectArtifactByteConst INSTANCE = new AspectArtifactByteConst();

    private AspectArtifactByteConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.BYTE_CONST;
    }
}
