package compilerxyz.main.eval.value.ubyte;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactUbyteConst implements AspectArtifact {
    public static final AspectArtifactUbyteConst INSTANCE = new AspectArtifactUbyteConst();

    private AspectArtifactUbyteConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.UBYTE_CONST;
    }
}
