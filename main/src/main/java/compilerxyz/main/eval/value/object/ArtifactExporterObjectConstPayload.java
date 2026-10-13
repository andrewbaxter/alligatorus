package compilerxyz.main.eval.value.object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.Imported;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.Opt;

public record ArtifactExporterObjectConstPayload(Type payloadType) implements ArtifactExporter {
    @Override
    public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, Opt<Type> ofType, Object self) {
        var dataRes = Artifactable.exportData(xx, loc, payloadType, self);
        if (dataRes.hasErrors()) return dataRes.reerr();
        return ExporterRoot.OBJECT_CONST_PAYLOAD.artifactExporterExport(
                xx, loc, ofType, dataRes.requireValue("ArtifactExporterObjectConstPayload::export data"));
    }

    @Override
    public EResult<Imported> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
        return ExporterRoot.OBJECT_CONST_PAYLOAD.artifactExporterImportFromData(xx, loc, id, data);
    }
}
