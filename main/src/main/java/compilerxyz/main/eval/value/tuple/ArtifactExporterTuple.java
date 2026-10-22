package compilerxyz.main.eval.value.tuple;

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
import compilerxyz.main.gen.codec.ArtifactTupleDataCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.Opt;

public record ArtifactExporterTuple(Tuple type) implements ArtifactExporter {

    @Override
    public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, Object self) {
        var liftRes = GenAspectLift.get(xx, loc, type);
        if (liftRes.hasErrors()) return liftRes.reerr();
        var liftedRes = liftRes.requireValue("Tuple::export lift aspect").aspectLiftLift(xx, loc, self);
        if (liftedRes.hasErrors()) return liftedRes.reerr();
        var erb = new Erb(loc);
        var typeArt = erb.mustErMaybeUnreachable(Exporters.exportType(xx, loc, type));
        var valueArt = erb.mustErMaybeUnreachable(
                ExporterRoot.TUPLE.artifactExporterExport(xx, loc, liftedRes.requireValue("Tuple::export lifted")));
        if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
        var body = new ArtifactTupleData(typeArt.optUnwrap(), valueArt.optUnwrap());
        return EResult.ok(new Artifact.Inline(ExporterRoot.TUPLE_DATA.body(ArtifactTupleDataCodec.toValue(body))));
    }

    @Override
    public EResult<Object> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
        return ExporterRoot.TUPLE_DATA.artifactExporterImportFromData(xx, loc, id, data);
    }
}
