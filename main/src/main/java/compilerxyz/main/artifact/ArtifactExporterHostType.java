package compilerxyz.main.artifact;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;

public final class ArtifactExporterHostType implements ArtifactExporter {
    public static final ArtifactExporterHostType INSTANCE = new ArtifactExporterHostType();

    private ArtifactExporterHostType() {}

    @Override
    public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, Object self) {
        return Exporters.exportType(xx, loc, MustCast.mustCast(Type.class, self));
    }

    @Override
    public EResult<Object> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
        throw new IllegalStateException(
                "ArtifactExporterHostType::import: type hosts import through the type's own exporter");
    }
}
