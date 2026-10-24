package compilerxyz.main.artifact;

import compilerxyz.main.eval.ArtifactImportScope;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.langhelp.CacheDamaged;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ArtifactSupport {
    private ArtifactSupport() {}

    @BuiltinPath(value = "eval.artifact_import", scopeId = 5064L)
    public static EResult<Object> importArtifact(ExprContext xx, QExprId loc, Artifact artifact) {
        var scope = ArtifactImportScope.of(xx.vx.ix);
        scope.depth++;
        try {
            Opt<ArtifactIdSource> id;
            ArtifactBody body;
            switch (artifact) {
                case Artifact.Inline b -> {
                    id = Opt.none();
                    body = b.body();
                }
                case Artifact.Ref r -> {
                    id = Opt.some(r.id());
                    var imported = scope.imported.getOpt(r.id());
                    if (imported.optIsSome()) {
                        return EResult.ok(imported.optUnwrap());
                    }
                    body = artifactBody(xx, r.id());
                }
            }
            var imported = importArtifactBody(xx, loc, id, body);
            if (id.optIsSome()
                    && !imported.hasErrors()
                    && scope.imported.getOpt(id.optUnwrap()).optIsNone()) {
                scope.imported.put(id.optUnwrap(), imported.requireValue("ArtifactSupport::importArtifact imported"));
            }
            return imported;
        } finally {
            scope.depth--;
            if (scope.depth == 0) scope.imported.clear();
        }
    }

    @BuiltinPath(value = "eval.artifact_import_early", scopeId = 5078L)
    public static void artifactImportEarly(ExprContext xx, ArtifactIdSource id, Object value) {
        var scope = ArtifactImportScope.of(xx.vx.ix);
        if (scope.depth == 0) {
            throw new IllegalStateException("artifact_import_early: no artifact import is running");
        }
        scope.imported.put(id, value);
    }

    private static ArtifactBody artifactBody(ExprContext xx, ArtifactIdSource id) {
        var got = xx.vx.ix.artifacts.artifactsGet(id);
        if (got.optIsNone()) throw new CacheDamaged("Unknown artifact: " + id);
        return got.optUnwrap();
    }

    private static EResult<Object> importArtifactBody(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, ArtifactBody body) {
        var errors = new TList<ExprErr>();
        ArtifactExporter exporter = null;
        ArtifactBodyExporter bex = body.exporter();
        switch (bex) {
            case ArtifactBodyExporter.Root r -> {
                exporter = ExporterRoot.ROOT;
            }
            case ArtifactBodyExporter.Body bb -> {
                var nested = importArtifactBody(xx, loc, Opt.none(), bb.inner());
                if (nested.hasErrors()) {
                    nested.drainInto(errors);
                    return errsOrEmpty(errors);
                }
                var v = nested.requireValue("ArtifactSupport::importArtifactBody Body nested");
                exporter = MustCast.mustCast(ArtifactExporter.class, v);
            }
            case ArtifactBodyExporter.SourceRef sr -> {
                Object exporterAny;
                var imported = ArtifactImportScope.of(xx.vx.ix).imported.getOpt(sr.id());
                if (imported.optIsSome()) {
                    exporterAny = imported.optUnwrap();
                } else {
                    var inner = importArtifactBody(xx, loc, Opt.some(sr.id()), artifactBody(xx, sr.id()));
                    if (inner.hasErrors()) {
                        inner.drainInto(errors);
                        return errsOrEmpty(errors);
                    }
                    exporterAny = inner.requireValue("ArtifactSupport::importArtifactBody SourceRef inner");
                }
                exporter = MustCast.mustCast(ArtifactExporter.class, exporterAny);
            }
        }
        return exporter.artifactExporterImportFromData(xx, loc, id, body.data());
    }

    public static EResult<Artifact> implExportIndependentArtifact(
            ExprContext xx, ArtifactIdHolder cell, Supplier<EResult<ArtifactBody>> produce) {
        if (cell.id().optIsSome()) {
            return EResult.ok(new Artifact.Ref(cell.id().optUnwrap()));
        }
        var artifacts = xx.vx.ix.artifacts;
        var id = artifacts.artifactsReserveId(xx.vx.ix.compileId());
        cell.set(id);
        var bodyRes = produce.get();
        if (bodyRes.hasErrors()) return bodyRes.reerr();
        artifacts.artifactsSet(id, bodyRes.requireValue("ArtifactSupport::implExportIndependentArtifact body"));
        return EResult.ok(new Artifact.Ref(id));
    }

    public static <T> EResult<Object> implImportIndependentArtifact(
            ExprContext xx,
            QExprId loc,
            ArtifactIdSource id,
            JsonValue data,
            Function<JsonValue, Result<T, String>> deserialize,
            Function<T, Result<ImportIndependentResult, String>> create) {
        var deser = deserialize.apply(data);
        T payload;
        switch (deser) {
            case Result.Err<T, String> err -> {
                return EResult.err(loc, err.error());
            }
            case Result.Ok<T, String> ok -> payload = ok.value();
        }
        var created = create.apply(payload);
        ImportIndependentResult triple;
        switch (created) {
            case Result.Err<ImportIndependentResult, String> err -> {
                return EResult.err(loc, err.error());
            }
            case Result.Ok<ImportIndependentResult, String> ok -> triple = ok.value();
        }
        if (triple.early().optIsSome()) {
            ArtifactImportScope.of(xx.vx.ix).imported.put(id, triple.early().optUnwrap());
        }
        var errors = new TList<ExprErr>();
        for (var dep : triple.dependencies()) {
            var depRes = importArtifact(xx, loc, dep);
            depRes.drainInto(errors);
        }
        var value = triple.finalizer().finalizerRun(xx, loc);
        ArtifactImportScope.of(xx.vx.ix).imported.put(id, value);
        return errors.isEmpty() ? EResult.ok(value) : EResult.mixed(value, errors);
    }

    public static EResult<Object> independentFinalizeImportArtifact(ExprContext xx, QExprId loc, Artifact artifact) {
        return switch (artifact) {
            case Artifact.Ref r -> EResult.ok(lookupRef(xx, r.id()));
            case Artifact.Inline b -> walkBody(xx, loc, b.body());
        };
    }

    public static EResult<Type> independentFinalizeImportTypeArtifact(ExprContext xx, QExprId loc, Artifact artifact) {
        var errors = new TList<ExprErr>();
        var res = independentFinalizeImportArtifact(xx, loc, artifact);
        if (res.hasErrors()) {
            res.drainInto(errors);
            return errsOrEmpty(errors);
        }
        return EResult.ok(MustCast.mustCast(
                Type.class, res.requireValue("ArtifactSupport::independentFinalizeImportTypeArtifact imported")));
    }

    private static Object lookupRef(ExprContext xx, ArtifactIdSource id) {
        var imported = ArtifactImportScope.of(xx.vx.ix).imported.getOpt(id);
        if (imported.optIsNone()) {
            throw new IllegalStateException(
                    "independentFinalizeImportArtifact: artifact " + id + " was not imported as a dependency");
        }
        return imported.optUnwrap();
    }

    private static EResult<Object> walkBody(ExprContext xx, QExprId loc, ArtifactBody body) {
        var errors = new TList<ExprErr>();
        return switch (body.exporter()) {
            case ArtifactBodyExporter.Root r -> {
                var res = ExporterRoot.ROOT.artifactExporterImportFromData(xx, loc, Opt.none(), body.data());
                res.drainInto(errors);
                yield errors.isEmpty() ? res : EResult.errs(errors);
            }
            case ArtifactBodyExporter.Body bb -> {
                var inner = walkBody(xx, loc, bb.inner());
                if (inner.hasErrors()) {
                    inner.drainInto(errors);
                    yield errsOrEmpty(errors);
                }
                var v = inner.requireValue("ArtifactSupport::walkBody Body inner");
                var exporter = MustCast.mustCast(ArtifactExporter.class, v);
                var res = exporter.artifactExporterImportFromData(xx, loc, Opt.none(), body.data());
                res.drainInto(errors);
                yield errors.isEmpty() ? res : EResult.errs(errors);
            }
            case ArtifactBodyExporter.SourceRef sr -> {
                var exporter = MustCast.mustCast(ArtifactExporter.class, lookupRef(xx, sr.id()));
                var res = exporter.artifactExporterImportFromData(xx, loc, Opt.none(), body.data());
                res.drainInto(errors);
                yield errors.isEmpty() ? res : EResult.errs(errors);
            }
        };
    }

    private static <T> EResult<T> errsOrEmpty(TList<ExprErr> errors) {
        return EResult.errs(errors);
    }

    @BuiltinFull(uniqueId = 6413L)
    public static final class ArtifactIdHolder {
        private Opt<ArtifactIdSource> id = Opt.none();

        public Opt<ArtifactIdSource> id() {
            return id;
        }

        public void set(ArtifactIdSource id) {
            this.id = Opt.some(id);
        }
    }

    public record ImportIndependentResult(Opt<Object> early, TList<Artifact> dependencies, Finalizer finalizer) {
        @FunctionalInterface
        public interface Finalizer {
            Object finalizerRun(ExprContext xx, QExprId loc);
        }
    }
}
