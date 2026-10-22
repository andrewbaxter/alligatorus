package compilerxyz.main.eval.value.record;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.Exporters;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.codec.ArtifactRecordDataCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.Opt;

public record ArtifactExporterRecord(Record type) implements ArtifactExporter {

    @Override
    public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, Object self) {
        var liftRes = GenAspectLift.get(xx, loc, type);
        if (liftRes.hasErrors()) return liftRes.reerr();
        var liftedRes = liftRes.requireValue("Record::export lift aspect").aspectLiftLift(xx, loc, self);
        if (liftedRes.hasErrors()) return liftedRes.reerr();
        var erb = new Erb(loc);
        var typeArt = erb.mustErMaybeUnreachable(Exporters.exportType(xx, loc, type));
        var valueArt = erb.mustErMaybeUnreachable(
                ExporterRoot.RECORD.artifactExporterExport(xx, loc, liftedRes.requireValue("Record::export lifted")));
        if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
        var body = new ArtifactRecordData(typeArt.optUnwrap(), valueArt.optUnwrap());
        return EResult.ok(new Artifact.Inline(ExporterRoot.RECORD_DATA.body(ArtifactRecordDataCodec.toValue(body))));
    }

    @Override
    public EResult<Object> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
        return ExporterRoot.RECORD_DATA.artifactExporterImportFromData(xx, loc, id, data);
    }
}
