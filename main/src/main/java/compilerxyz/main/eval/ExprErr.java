package compilerxyz.main.eval;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.gen.codec.ArtifactExprErrCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6027L, exporter = "EXPR_ERR")
@BuiltinPath("types.expr_err")
public record ExprErr(QExprId loc, TList<ExprSubErr> errors) {

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            var err = MustCast.mustCast(ExprErr.class, self);
            return EResult.ok(new Artifact.Inline(
                    makeBody.apply(ArtifactExprErrCodec.toValue(ArtifactExprErr.fromExprErr(err)))));
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactExprErr body;
            try {
                body = ArtifactExprErrCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(loc, "ExprErr data: failed to parse ArtifactExprErr: " + e.getMessage());
            }
            return EResult.ok(body.toExprErr());
        }
    };

    public static ExprErr opaque(QExprId loc, String message) {
        return new ExprErr(loc, TList.of(new ExprSubErr.Opaque(message)));
    }

    public static ExprErr importFailed(QExprId loc, SourceSpecAbsolute spec, String reason) {
        return new ExprErr(loc, TList.of(new ExprSubErr.ImportFailed(spec, reason)));
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
            sb.append(sub.exprSubErrMessage());
        }
        return sb.toString();
    }
}
