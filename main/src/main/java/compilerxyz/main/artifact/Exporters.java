package compilerxyz.main.artifact;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.Value;

public final class Exporters {
    private Exporters() {}

    public static EResult<Artifact> exportType(ExprContext xx, QExprId loc, Type type) {
        var exporter = type.typeArtifactExporter(xx);
        if (exporter.optIsNone()) return EResult.err(loc, "type " + type.typeDesc() + " is not exportable");
        return exporter.optUnwrap().artifactExporterExport(xx, loc, type);
    }

    public static EResult<Artifact> exportValue(ExprContext xx, QExprId loc, Value value) {
        var exporter = value.valueArtifactExporter(xx);
        if (exporter.optIsNone()) {
            return EResult.err(loc, "value of type " + value.valueType().typeDesc() + " is not exportable");
        }
        return exporter.optUnwrap().artifactExporterExport(xx, loc, value);
    }

    public static EResult<Artifact> exportData(ExprContext xx, QExprId loc, Type type, Object data) {
        var exporterRes = exporterOfType(xx, loc, type);
        if (exporterRes.hasErrors()) return exporterRes.reerr();
        return exporterRes.requireValue("Exporters::exportData exporter").artifactExporterExport(xx, loc, data);
    }

    public static EResult<ArtifactExporter> exporterOfType(ExprContext xx, QExprId loc, Type type) {
        var asp = GenAspectArtifact.get(xx, loc, type);
        if (asp.hasErrors()) return asp.reerr();
        return EResult.ok(asp.requireValue("Exporters::exporterOfType aspect").aspectArtifactExporter());
    }
}
