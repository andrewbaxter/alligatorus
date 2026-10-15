package compilerxyz.main.eval.value.empty;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.artifact.Exporters;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.Value;
import compilerxyz.main.gen.codec.ArtifactValueEmptyCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import java.util.function.Function;

public record ValueEmpty(Type type) implements Value {
    public Type valueType() {
        return type;
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            var value = MustCast.mustCast(ValueEmpty.class, self);
            var typeRes = Exporters.exportType(xx, loc, value.type);
            if (typeRes.hasErrors()) return typeRes.reerr();
            var body = new ArtifactValueEmpty(typeRes.requireValue("ValueEmpty::export type"));
            return EResult.ok(new Artifact.Body(makeBody.apply(ArtifactValueEmptyCodec.toValue(body))));
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactValueEmpty body;
            try {
                body = ArtifactValueEmptyCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(loc, "ValueEmpty data: failed to parse ArtifactValueEmpty: " + e.getMessage());
            }
            var typeRes = ArtifactSupport.importArtifact(xx, loc, body.type());
            if (typeRes.hasErrors()) return typeRes.reerr();
            return EResult.ok(
                    new ValueEmpty(MustCast.mustCast(Type.class, typeRes.requireValue("ValueEmpty::import type"))));
        }
    };

    @Override
    public EResult<ArtifactExporter> valueExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.VALUE_EMPTY);
    }
}
