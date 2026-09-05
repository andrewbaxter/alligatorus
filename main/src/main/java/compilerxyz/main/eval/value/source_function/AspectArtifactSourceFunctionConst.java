package compilerxyz.main.eval.value.source_function;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactSourceFunctionConst implements AspectArtifact {
    public static final AspectArtifactSourceFunctionConst INSTANCE = new AspectArtifactSourceFunctionConst();

    private AspectArtifactSourceFunctionConst() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.SOURCE_FUNCTION_CONST;
    }
}
