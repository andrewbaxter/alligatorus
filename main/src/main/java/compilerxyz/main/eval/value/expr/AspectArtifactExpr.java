package compilerxyz.main.eval.value.expr;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactExpr implements AspectArtifact {
    public static final AspectArtifactExpr INSTANCE = new AspectArtifactExpr();

    private AspectArtifactExpr() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.EXPR_VALUE;
    }
}
