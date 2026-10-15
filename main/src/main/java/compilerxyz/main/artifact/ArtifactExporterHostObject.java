package compilerxyz.main.artifact;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.Value;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.Opt;

public final class ArtifactExporterHostObject implements ArtifactExporter {
    public static final ArtifactExporterHostObject INSTANCE = new ArtifactExporterHostObject();

    private ArtifactExporterHostObject() {}

    @Override
    public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, Object self) {
        return switch (self) {
            case Type type -> Exporters.exportType(xx, loc, type);
            case Value value -> Exporters.exportValue(xx, loc, value);
            case ExportableObject object -> Exporters.exportObject(xx, loc, object);
            default -> EResult.err(loc, "host object " + self.getClass().getName() + " is not exportable");
        };
    }

    @Override
    public EResult<Object> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
        return EResult.err(loc, "a host object artifact names its own exporter");
    }
}
