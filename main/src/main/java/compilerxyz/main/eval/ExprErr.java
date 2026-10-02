package compilerxyz.main.eval;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.gen.codec.ArtifactExprErrCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6027L)
@BuiltinPath("types.expr_err")
public record ExprErr(QExprId loc, TList<ExprSubErr> errors) implements Artifactable {
    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.EXPR_ERR);
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var err = MustCast.mustCast(ExprErr.class, self);
            var messages = new TList<String>(err.errors().size());
            for (var sub : err.errors()) {
                switch (sub) {
                    case ExprSubErr.Opaque o -> messages.add(o.message());
                }
            }
            var body = new ArtifactExprErr(
                    new ArtifactExpr.ArtifactQExprId(
                            err.loc().source().value(), err.loc().expr()),
                    messages);
            return EResult.ok(new Artifact.Body(makeBody.apply(ArtifactExprErrCodec.toValue(body))));
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactExprErr body;
            try {
                body = ArtifactExprErrCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(loc, "ExprErr data: failed to parse ArtifactExprErr: " + e.getMessage());
            }
            var subs = new TList<ExprSubErr>(body.messages().size());
            for (var message : body.messages()) subs.add(new ExprSubErr.Opaque(message));
            return EResult.ok(new ExprErr(
                    new QExprId(new SourceId(body.loc().source()), body.loc().expr()), subs));
        }
    };

    public static ExprErr opaque(QExprId loc, String message) {
        return new ExprErr(loc, TList.of(new ExprSubErr.Opaque(message)));
    }

    public String message() {
        var sb = new StringBuilder();
        sb.append("[").append(loc.source().value());
        if (loc.expr().optIsSome()) {
            sb.append(":").append(loc.expr().optUnwrap());
        }
        sb.append("] ");
        for (var sub : errors) {
            if (!sb.toString().endsWith("] ")) sb.append("; ");
            switch (sub) {
                case ExprSubErr.Opaque o -> sb.append(o.message());
            }
        }
        return sb.toString();
    }
}
