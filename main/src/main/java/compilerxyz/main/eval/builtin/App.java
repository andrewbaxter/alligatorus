package compilerxyz.main.eval.builtin;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.codec.ArtifactAppCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import java.util.Base64;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6470L)
@BuiltinPath("types.app")
public record App(Opt<String> executor, byte[] bytes) implements Artifactable {
    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.APP);
    }

    public static ArtifactApp toArtifactApp(App app) {
        return new ArtifactApp(app.executor(), Base64.getEncoder().encodeToString(app.bytes()));
    }

    public static App fromArtifactApp(ArtifactApp body) {
        return new App(body.executor(), Base64.getDecoder().decode(body.bytesBase64()));
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var app = MustCast.mustCast(App.class, self);
            return EResult.ok(new Artifact.Body(makeBody.apply(ArtifactAppCodec.toValue(toArtifactApp(app)))));
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactApp body;
            try {
                body = ArtifactAppCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(loc, "App data: failed to parse ArtifactApp: " + e.getMessage());
            }
            return EResult.ok(fromArtifactApp(body));
        }
    };
}
