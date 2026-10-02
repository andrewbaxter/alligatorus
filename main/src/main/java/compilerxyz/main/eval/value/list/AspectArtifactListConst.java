package compilerxyz.main.eval.value.list;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactListConst implements AspectArtifact {
    public static final AspectArtifactListConst INSTANCE = new AspectArtifactListConst();

    private AspectArtifactListConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.LIST_CONST;
    }
}
