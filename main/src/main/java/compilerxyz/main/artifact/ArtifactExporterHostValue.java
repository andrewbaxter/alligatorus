package compilerxyz.main.artifact;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.Value;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;

public final class ArtifactExporterHostValue implements ArtifactExporter {
    public static final ArtifactExporterHostValue INSTANCE = new ArtifactExporterHostValue();

    private ArtifactExporterHostValue() {}

    @Override
    public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, Object self) {
        return Exporters.exportValue(xx, loc, MustCast.mustCast(Value.class, self));
    }

    @Override
    public EResult<Object> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
        throw new IllegalStateException(
                "ArtifactExporterHostValue::import: value hosts import through the value's own exporter");
    }
}
