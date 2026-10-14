package compilerxyz.main.artifact;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectDynamicConst;
import compilerxyz.main.gen.Value;

public final class Exporters {
    private Exporters() {}

    public static EResult<ArtifactExporter> typeExporter(QExprId loc, Type type) {
        var exporter = type.typeExporter();
        if (exporter.optIsNone()) return EResult.err(loc, "type " + type.typeDesc() + " is not exportable");
        return EResult.ok(exporter.optUnwrap());
    }

    public static EResult<Artifact> exportType(ExprContext xx, QExprId loc, Type type) {
        var exporterRes = typeExporter(loc, type);
        if (exporterRes.hasErrors()) return exporterRes.reerr();
        return exporterRes.requireValue("Exporters::exportType exporter").artifactExporterExport(xx, loc, type);
    }

    public static EResult<Artifact> exportValue(ExprContext xx, QExprId loc, Value value) {
        var exporterRes = value.valueExporter(xx, loc);
        if (exporterRes.hasErrors()) return exporterRes.reerr();
        return exporterRes.requireValue("Exporters::exportValue exporter").artifactExporterExport(xx, loc, value);
    }

    public static EResult<Artifact> exportObject(ExprContext xx, QExprId loc, ExportableObject object) {
        return object.exportableObjectExporter().artifactExporterExport(xx, loc, object);
    }

    public static EResult<Artifact> exportData(ExprContext xx, QExprId loc, Type type, Object data) {
        var exporterRes = exporterOfType(xx, loc, type);
        if (exporterRes.hasErrors()) return exporterRes.reerr();
        return exporterRes.requireValue("Exporters::exportData exporter").artifactExporterExport(xx, loc, data);
    }

    public static Type constFormOf(ExprContext xx, Type type) {
        var constForm = GenAspectDynamicConst.getMaybe(xx, type);
        if (constForm.optIsSome() && GenAspectArtifact.getMaybe(xx, type).optIsNone()) {
            return constForm.optUnwrap().aspectDynamicConstConstForm();
        }
        return type;
    }

    public static EResult<ArtifactExporter> exporterOfType(ExprContext xx, QExprId loc, Type type) {
        var asp = GenAspectArtifact.get(xx, loc, constFormOf(xx, type));
        if (asp.hasErrors()) return asp.reerr();
        return EResult.ok(asp.requireValue("Exporters::exporterOfType aspect").aspectArtifactExporter());
    }
}
