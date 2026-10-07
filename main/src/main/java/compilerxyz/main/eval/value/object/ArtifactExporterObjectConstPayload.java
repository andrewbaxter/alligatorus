package compilerxyz.main.eval.value.object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;

public record ArtifactExporterObjectConstPayload(Type payloadType) implements ArtifactExporter {
    @Override
    public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, Artifactable self) {
        var voc = MustCast.mustCast(ValueObjectConst.class, self);
        var liftRes = GenAspectLift.get(xx, loc, payloadType);
        if (liftRes.hasErrors()) return liftRes.reerr();
        var valueRes = liftRes.requireValue("ArtifactExporterObjectConstPayload::lift aspect")
                .aspectLiftLift(xx, loc, voc.data());
        if (valueRes.hasErrors()) return valueRes.reerr();
        return ExporterRoot.OBJECT_CONST_PAYLOAD.artifactExporterExport(
                xx,
                loc,
                new ValueObjectConst(voc.type(), valueRes.requireValue("ArtifactExporterObjectConstPayload::lift")));
    }

    @Override
    public EResult<Artifactable> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
        return ExporterRoot.OBJECT_CONST_PAYLOAD.artifactExporterImportFromData(xx, loc, id, data);
    }
}
