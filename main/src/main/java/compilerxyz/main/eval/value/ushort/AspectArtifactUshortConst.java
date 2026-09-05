package compilerxyz.main.eval.value.ushort;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactUshortConst implements AspectArtifact {
    public static final AspectArtifactUshortConst INSTANCE = new AspectArtifactUshortConst();

    private AspectArtifactUshortConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.USHORT_CONST;
    }
}
