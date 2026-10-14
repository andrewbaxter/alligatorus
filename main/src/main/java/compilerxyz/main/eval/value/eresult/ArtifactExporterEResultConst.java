package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.Exporters;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.GenExprErr;
import compilerxyz.main.gen.codec.ArtifactEResultValueCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record ArtifactExporterEResultConst<T extends Type>(TypeEResultConst<T> type) implements ArtifactExporter {

    @Override
    public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, Object self) {
        var host = MustCast.mustCast(EResult.class, self);
        var erb = new Erb(loc);
        var elementTypeArt = erb.mustErMaybeUnreachable(Exporters.exportType(xx, loc, type.elementType));
        Opt<Artifact> okArt = Opt.none();
        var errArts = new TList<Artifact>();
        if (host.hasErrors()) {
            for (var e : MustCast.mustCast(TList.class, host.errorsOpt().optUnwrap())) {
                var art = erb.mustErMaybeUnreachable(Exporters.exportData(xx, loc, GenExprErr.dynamicType(), e));
                if (art.optIsSome()) errArts.add(art.optUnwrap());
            }
        } else {
            var art = erb.mustErMaybeUnreachable(Exporters.exportData(
                    xx, loc, type.elementType, host.requireValue("TypeEResultConst::export ok host")));
            if (art.optIsSome()) okArt = Opt.some(art.optUnwrap());
        }
        if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
        var body = new ArtifactEResultValue(elementTypeArt.optUnwrap(), okArt, errArts);
        return EResult.ok(new Artifact.Body(ExporterRoot.ERESULT_CONST.body(ArtifactEResultValueCodec.toValue(body))));
    }

    @Override
    public EResult<Object> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
        return ExporterRoot.ERESULT_CONST.artifactExporterImportFromData(xx, loc, id, data);
    }
}
