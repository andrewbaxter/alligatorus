package compilerxyz.main.eval;

import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.TList;

public record ArtifactExprErr(ArtifactExpr.ArtifactQExprId loc, TList<Sub> errors) implements SafeJsonSerializable {
    public sealed interface Sub {
        record Opaque(String message) implements Sub {}

        record ImportFailed(String spec, String reason) implements Sub {}
    }

    public static ArtifactExprErr fromExprErr(ExprErr err) {
        var subs = new TList<Sub>(err.errors().size());
        for (var sub : err.errors()) {
            subs.add(
                    switch (sub) {
                        case ExprSubErr.Opaque o -> new Sub.Opaque(o.message());
                        case ExprSubErr.ImportFailed f ->
                            new Sub.ImportFailed(f.spec().sourceSpecAbsoluteSpecKey(), f.reason());
                    });
        }
        return new ArtifactExprErr(
                new ArtifactExpr.ArtifactQExprId(
                        err.loc().source().value(), err.loc().expr()),
                subs);
    }

    public ExprErr toExprErr() {
        var subs = new TList<ExprSubErr>(errors.size());
        for (var sub : errors) {
            subs.add(
                    switch (sub) {
                        case Sub.Opaque o -> new ExprSubErr.Opaque(o.message());
                        case Sub.ImportFailed f ->
                            new ExprSubErr.ImportFailed(
                                    SourceSpecAbsolute.sourceSpecAbsoluteParseKey(f.spec()), f.reason());
                    });
        }
        return new ExprErr(new QExprId(new SourceId(loc.source()), loc.expr()), subs);
    }
}
