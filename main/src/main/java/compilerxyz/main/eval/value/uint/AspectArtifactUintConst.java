package compilerxyz.main.eval.value.uint;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactUintConst implements AspectArtifact {
    public static final AspectArtifactUintConst INSTANCE = new AspectArtifactUintConst();

    private AspectArtifactUintConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.UINT_CONST;
    }
}
