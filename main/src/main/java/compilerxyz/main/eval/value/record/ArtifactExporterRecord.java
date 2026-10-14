package compilerxyz.main.eval.value.record;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.Opt;

public record ArtifactExporterRecord(Record type) implements ArtifactExporter {

    @Override
    public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, Object self) {
        if (self instanceof Record) return ExporterRoot.RECORD.artifactExporterExport(xx, loc, self);
        var liftRes = GenAspectLift.get(xx, loc, type);
        if (liftRes.hasErrors()) return liftRes.reerr();
        var liftedRes = liftRes.requireValue("Record::export lift aspect").aspectLiftLift(xx, loc, self);
        if (liftedRes.hasErrors()) return liftedRes.reerr();
        return ExporterRoot.RECORD.artifactExporterExport(xx, loc, liftedRes.requireValue("Record::export lifted"));
    }

    @Override
    public EResult<Object> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
        return ExporterRoot.RECORD.artifactExporterImportFromData(xx, loc, id, data);
    }
}
