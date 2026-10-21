package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.Opt;

public record ArtifactExporterSourceObject(TypeSourceObjectDynamic type) implements ArtifactExporter {
    @Override
    public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, Object self) {
        var emitted = type.exporter(xx, loc);
        if (emitted.hasErrors()) return emitted.reerr();
        return emitted.requireValue("ArtifactExporterSourceObject::export emitted")
                .artifactExporterExport(xx, loc, self);
    }

    @Override
    public EResult<Object> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
        var emitted = type.exporter(xx, loc);
        if (emitted.hasErrors()) return emitted.reerr();
        return emitted.requireValue("ArtifactExporterSourceObject::import emitted")
                .artifactExporterImportFromData(xx, loc, id, data);
    }
}
