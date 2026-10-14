package compilerxyz.main.eval.value.object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.Exporters;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.Opt;

public record ArtifactExporterObjectConstPayload(Type payloadType) implements ArtifactExporter {
    @Override
    public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, Object self) {
        return Exporters.exportData(xx, loc, payloadType, self);
    }

    @Override
    public EResult<Object> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
        var exporterRes = Exporters.exporterOfType(xx, loc, payloadType);
        if (exporterRes.hasErrors()) return exporterRes.reerr();
        return exporterRes
                .requireValue("ArtifactExporterObjectConstPayload::import exporter")
                .artifactExporterImportFromData(xx, loc, id, data);
    }
}
