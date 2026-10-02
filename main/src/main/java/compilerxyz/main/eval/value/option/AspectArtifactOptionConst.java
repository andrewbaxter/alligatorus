package compilerxyz.main.eval.value.option;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactOptionConst implements AspectArtifact {
    public static final AspectArtifactOptionConst INSTANCE = new AspectArtifactOptionConst();

    private AspectArtifactOptionConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.OPTION_CONST;
    }
}
