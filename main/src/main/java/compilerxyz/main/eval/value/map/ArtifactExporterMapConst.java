package compilerxyz.main.eval.value.map;

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
import compilerxyz.main.gen.codec.ArtifactMapValueCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.Map;

public record ArtifactExporterMapConst<K extends Type, V extends Type>(TypeMapConst<K, V> type)
        implements ArtifactExporter {

    @Override
    public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, Object self) {
        var host = MustCast.mustCast(TOrderedMap.class, self);
        var erb = new Erb(loc);
        var keyTypeArt = erb.mustErMaybeUnreachable(Exporters.exportType(xx, loc, type.keyType));
        var valueTypeArt = erb.mustErMaybeUnreachable(Exporters.exportType(xx, loc, type.valueType));
        var entryArts = new TList<ArtifactMapEntry>();
        for (var raw : host.entrySet()) {
            var entry = MustCast.mustCast(Map.Entry.class, raw);
            var keyArt = erb.mustErMaybeUnreachable(Exporters.exportData(xx, loc, type.keyType, entry.getKey()));
            var valueArt = erb.mustErMaybeUnreachable(Exporters.exportData(xx, loc, type.valueType, entry.getValue()));
            if (keyArt.optIsSome() && valueArt.optIsSome()) {
                entryArts.add(new ArtifactMapEntry(keyArt.optUnwrap(), valueArt.optUnwrap()));
            }
        }
        if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
        var body = new ArtifactMapValue(keyTypeArt.optUnwrap(), valueTypeArt.optUnwrap(), entryArts);
        return EResult.ok(new Artifact.Inline(ExporterRoot.MAP_CONST.body(ArtifactMapValueCodec.toValue(body))));
    }

    @Override
    public EResult<Object> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
        return ExporterRoot.MAP_CONST.artifactExporterImportFromData(xx, loc, id, data);
    }
}
