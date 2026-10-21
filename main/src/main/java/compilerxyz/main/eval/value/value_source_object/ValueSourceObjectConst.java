package compilerxyz.main.eval.value.value_source_object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.main.gen.codec.ValueSourceObjectConstArtifactValueSourceObjectCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import java.util.function.Function;

public final class ValueSourceObjectConst {
    private ValueSourceObjectConst() {}

    public record ArtifactValueSourceObject(Artifact typeArtifact, Artifact underlying)
            implements SafeJsonSerializable {}

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            return EResult.err(loc, "a source object instance is exported through its type");
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            return importInstance(xx, loc, data);
        }
    };

    private static EResult<Object> importInstance(ExprContext xx, QExprId loc, JsonValue data) {
        ArtifactValueSourceObject body;
        try {
            body = ValueSourceObjectConstArtifactValueSourceObjectCodec.fromValue(data);
        } catch (RuntimeException e) {
            return EResult.err(
                    loc,
                    "ValueSourceObjectConst::exporterRootDelegateSelfFromArtifactData: failed to parse "
                            + "ArtifactValueSourceObject: "
                            + e.getMessage());
        }
        var typeRes = ArtifactSupport.importArtifact(xx, loc, body.typeArtifact());
        if (typeRes.hasErrors()) return typeRes.reerr();
        var dt = MustCast.mustCast(
                TypeSourceObjectDynamic.class, typeRes.requireValue("ValueSourceObjectConst::import type"));
        var exporter = dt.exporter(xx, loc);
        if (exporter.hasErrors()) return exporter.reerr();
        return exporter.requireValue("ValueSourceObjectConst::import exporter")
                .artifactExporterImportFromData(xx, loc, Opt.none(), data);
    }
}
