package compilerxyz.main.eval.value.source_macro;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactSourceMacroConst implements AspectArtifact {
    public static final AspectArtifactSourceMacroConst INSTANCE = new AspectArtifactSourceMacroConst();

    private AspectArtifactSourceMacroConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.SOURCE_MACRO_CONST;
    }
}
