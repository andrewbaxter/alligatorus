package compilerxyz.main.eval.value.list;

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
import compilerxyz.main.gen.codec.ArtifactListValueCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record ArtifactExporterListConst<T extends Type>(TypeListConst<T> type) implements ArtifactExporter {

    @Override
    public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, Object self) {
        var erb = new Erb(loc);
        var elementTypeArt = erb.mustErMaybeUnreachable(Exporters.exportType(xx, loc, type.elementType()));
        var elementArts = new TList<Artifact>();
        for (var host : TypeListConst.hostElements(self)) {
            var elementArt = erb.mustErMaybeUnreachable(Exporters.exportData(xx, loc, type.elementType(), host));
            if (elementArt.optIsSome()) elementArts.add(elementArt.optUnwrap());
        }
        if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
        var body = new ArtifactListValue(elementTypeArt.optUnwrap(), elementArts);
        return EResult.ok(new Artifact.Inline(ExporterRoot.LIST_CONST.body(ArtifactListValueCodec.toValue(body))));
    }

    @Override
    public EResult<Object> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
        return ExporterRoot.LIST_CONST.artifactExporterImportFromData(xx, loc, id, data);
    }
}
