package compilerxyz.main.eval.value.option;

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
import compilerxyz.main.gen.codec.ArtifactOptionValueCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;

public record ArtifactExporterOptionConst<T extends Type>(TypeOptionConst<T> type) implements ArtifactExporter {

    @Override
    public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, Object self) {
        var erb = new Erb(loc);
        var elementTypeArt = erb.mustErMaybeUnreachable(Exporters.exportType(xx, loc, type.elementType));
        Opt<Artifact> payloadArt = Opt.none();
        Opt<Object> payload;
        if (self instanceof OptionStore store) {
            payload = store.optionStoreBoxed();
        } else {
            var opt = MustCast.mustCast(Opt.class, self);
            payload = opt.optIsSome() ? Opt.some(opt.optUnwrap()) : Opt.none();
        }
        if (payload.optIsSome()) {
            var art = erb.mustErMaybeUnreachable(Exporters.exportData(xx, loc, type.elementType, payload.optUnwrap()));
            if (art.optIsSome()) payloadArt = Opt.some(art.optUnwrap());
        }
        if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
        var body = new ArtifactOptionValue(elementTypeArt.optUnwrap(), payloadArt);
        return EResult.ok(new Artifact.Body(ExporterRoot.OPTION_CONST.body(ArtifactOptionValueCodec.toValue(body))));
    }

    @Override
    public EResult<Object> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
        return ExporterRoot.OPTION_CONST.artifactExporterImportFromData(xx, loc, id, data);
    }
}
