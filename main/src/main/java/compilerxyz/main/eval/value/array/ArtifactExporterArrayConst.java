package compilerxyz.main.eval.value.array;

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
import compilerxyz.main.gen.codec.ArtifactArrayValueCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.lang.reflect.Array;

public record ArtifactExporterArrayConst<T extends Type>(TypeArrayConst<T> type) implements ArtifactExporter {

    @Override
    public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, Object self) {
        var erb = new Erb(loc);
        var elementTypeArt = erb.mustErMaybeUnreachable(Exporters.exportType(xx, loc, type.elementType));
        var elementArts = new TList<Artifact>();
        for (int i = 0; i < Array.getLength(self); i++) {
            var elementArt =
                    erb.mustErMaybeUnreachable(Exporters.exportData(xx, loc, type.elementType, Array.get(self, i)));
            if (elementArt.optIsSome()) elementArts.add(elementArt.optUnwrap());
        }
        if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
        var body = new ArtifactArrayValue(elementTypeArt.optUnwrap(), elementArts);
        return EResult.ok(new Artifact.Body(ExporterRoot.ARRAY_CONST.body(ArtifactArrayValueCodec.toValue(body))));
    }

    @Override
    public EResult<Object> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
        return ExporterRoot.ARRAY_CONST.artifactExporterImportFromData(xx, loc, id, data);
    }
}
