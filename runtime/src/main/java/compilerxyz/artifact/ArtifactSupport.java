package compilerxyz.artifact;

import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public final class ArtifactSupport {
  private ArtifactSupport() {}

  public static ExporterAny valueAsExporterAny(Value v) {
    if (v instanceof ExporterAny ea) return ea;
    throw new IllegalStateException(
        "Value " + v.getClass().getName() + " does not implement ExporterAny");
  }

  public static EResult<Artifact> exportArtifact(ExprContext xx, QExprId loc, Value v) {
    var errors = new TList<ExprErr>();
    var t = v.valueType();
    var asp = AspectArtifact.get(xx, loc, t);
    if (asp.hasErrors()) {
      asp.drainInto(errors);
      return errsOrEmpty(errors);
    }
    var aspVal = asp.requireValue("ArtifactSupport::exportArtifact aspect");
    var exporter = aspVal.aspectArtifactExporter();
    var art = exporter.artifactExporterExport(xx, loc, valueAsExporterAny(v));
    if (art.hasErrors()) {
      art.drainInto(errors);
      return errsOrEmpty(errors);
    }
    var artVal = art.requireValue("ArtifactSupport::exportArtifact artifact");
    return EResult.ok(artVal);
  }

  public static EResult<ArtifactExporter> exporterAnyToArtifactExporter(
      ExprContext xx, QExprId loc, ExporterAny exporterAny) {
    if (exporterAny instanceof ExporterRoot er) {
      return EResult.ok(er);
    }
    var errors = new TList<ExprErr>();
    var typeRes = exporterAny.exporterAnyAsType();
    if (typeRes.hasErrors()) {
      typeRes.drainInto(errors);
      return errsOrEmpty(errors);
    }
    var type = typeRes.requireValue("ArtifactSupport::exporterAnyToArtifactExporter type");
    var aspRes = AspectArtifact.get(xx, loc, type);
    if (aspRes.hasErrors()) {
      aspRes.drainInto(errors);
      return errsOrEmpty(errors);
    }
    var asp = aspRes.requireValue("ArtifactSupport::exporterAnyToArtifactExporter aspect");
    return EResult.ok(asp.aspectArtifactExporter());
  }

  public static EResult<ExporterAny> importArtifact(
      ExprContext xx, QExprId loc, Artifact artifact) {
    @Nullable ArtifactIdSource id;
    ArtifactBody body;
    switch (artifact) {
      case Artifact.Body b -> {
        id = null;
        body = b.body();
      }
      case Artifact.Ref r -> {
        id = r.id();
        var cached = xx.vx.ix.deserializedArtifacts.getOpt(r.id());
        if (cached instanceof Opt.Some<ExporterAny> some) {
          return EResult.ok(some.value());
        }
        var got = xx.vx.ix.gx.artifacts.artifactsGet(r.id());
        if (got instanceof Result.Err<ArtifactBody, String> err) {
          return EResult.err(loc, err.error());
        }
        body = ((Result.Ok<ArtifactBody, String>) got).value();
      }
    }
    return importArtifactBody(xx, loc, Opt.ofNullable(id), body);
  }

  private static EResult<ExporterAny> importArtifactBody(
      ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, ArtifactBody body) {
    var errors = new TList<ExprErr>();
    @Nullable ArtifactExporter exporter = null;
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
        var exp = exporterAnyToArtifactExporter(xx, loc, v);
        if (exp.hasErrors()) {
          exp.drainInto(errors);
          return errsOrEmpty(errors);
        }
        exporter = exp.requireValue("ArtifactSupport::importArtifactBody Body exporter");
      }
      case ArtifactBodyExporter.SourceRef sr -> {
        ExporterAny exporterAny;
        var cached = xx.vx.ix.deserializedArtifacts.getOpt(sr.id());
        if (cached instanceof Opt.Some<ExporterAny> some) {
          exporterAny = some.value();
        } else {
          var got = xx.vx.ix.gx.artifacts.artifactsGet(sr.id());
          if (got instanceof Result.Err<ArtifactBody, String> err) {
            return EResult.err(loc, err.error());
          }
          var inner = importArtifactBody(xx, loc, Opt.some(sr.id()),
              ((Result.Ok<ArtifactBody, String>) got).value());
          if (inner.hasErrors()) {
            inner.drainInto(errors);
            return errsOrEmpty(errors);
          }
          exporterAny =
              inner.requireValue("ArtifactSupport::importArtifactBody SourceRef inner");
        }
        var exp = exporterAnyToArtifactExporter(xx, loc, exporterAny);
        if (exp.hasErrors()) {
          exp.drainInto(errors);
          return errsOrEmpty(errors);
        }
        exporter = exp.requireValue("ArtifactSupport::importArtifactBody SourceRef exporter");
      }
    }
    return exporter.artifactExporterImportFromData(xx, loc, id, body.data());
  }

  public static EResult<Type> importTypeArtifact(
      ExprContext xx, QExprId loc, Artifact artifact) {
    var errors = new TList<ExprErr>();
    var res = importArtifact(xx, loc, artifact);
    if (res.hasErrors()) {
      res.drainInto(errors);
      return errsOrEmpty(errors);
    }
    var v = res.requireValue("ArtifactSupport::importTypeArtifact imported");
    var exporterAnyAsType = v.exporterAnyAsType();
    if (exporterAnyAsType.hasErrors()) {
      exporterAnyAsType.drainInto(errors);
      return errsOrEmpty(errors);
    }
    var t = exporterAnyAsType.requireValue("ArtifactSupport::importTypeArtifact type");
    return EResult.ok(t);
  }

  public static Artifact implExportIndependentArtifact(
      Artifacts artifacts, ArtifactIdHolder cell, Supplier<ArtifactBody> produce) {
    if (cell.id() != null) {
      return new Artifact.Ref(cell.id());
    }
    var id = artifacts.artifactsReserveId();
    cell.set(id);
    artifacts.artifactsSet(id, produce.get());
    return new Artifact.Ref(id);
  }

  public static <T> EResult<ExporterAny> implImportIndependentArtifact(
      ExprContext xx,
      QExprId loc,
      ArtifactIdSource id,
      JsonValue data,
      Function<JsonValue, Result<T, String>> deserialize,
      Function<T, Result<ImportIndependentResult, String>> create) {
    var deser = deserialize.apply(data);
    if (deser instanceof Result.Err<T, String> err) {
      return EResult.err(loc, err.error());
    }
    var payload = ((Result.Ok<T, String>) deser).value();
    var created = create.apply(payload);
    if (created instanceof Result.Err<ImportIndependentResult, String> err) {
      return EResult.err(loc, err.error());
    }
    var triple = ((Result.Ok<ImportIndependentResult, String>) created).value();
    xx.vx.ix.deserializedArtifacts.put(id, triple.value());
    var errors = new TList<ExprErr>();
    for (var dep : triple.dependencies()) {
      var depRes = importArtifact(xx, loc, dep);
      depRes.drainInto(errors);
    }
    triple.finalizer().run(xx, loc);
    return errors.isEmpty() ? EResult.ok(triple.value()) : drainOk(triple.value(), errors);
  }

  public static EResult<ExporterAny> independentFinalizeImportArtifact(
      ExprContext xx, QExprId loc, Artifact artifact) {
    return switch (artifact) {
      case Artifact.Ref r -> lookupRef(xx, loc, r.id());
      case Artifact.Body b -> walkBody(xx, loc, b.body());
    };
  }

  public static EResult<Type> independentFinalizeImportTypeArtifact(
      ExprContext xx, QExprId loc, Artifact artifact) {
    var errors = new TList<ExprErr>();
    var res = independentFinalizeImportArtifact(xx, loc, artifact);
    if (res.hasErrors()) {
      res.drainInto(errors);
      return errsOrEmpty(errors);
    }
    var v = res.requireValue("ArtifactSupport::independentFinalizeImportTypeArtifact imported");
    var exporterAnyAsType = v.exporterAnyAsType();
    if (exporterAnyAsType.hasErrors()) {
      exporterAnyAsType.drainInto(errors);
      return errsOrEmpty(errors);
    }
    var t =
        exporterAnyAsType.requireValue(
            "ArtifactSupport::independentFinalizeImportTypeArtifact type");
    return EResult.ok(t);
  }

  private static EResult<ExporterAny> lookupRef(
      ExprContext xx, QExprId loc, ArtifactIdSource id) {
    var cached = xx.vx.ix.deserializedArtifacts.getOpt(id);
    if (cached instanceof Opt.Some<ExporterAny> some) {
      return EResult.ok(some.value());
    }
    var cachedIds = new TList<ArtifactIdSource>();

    for (var entry : xx.vx.ix.deserializedArtifacts.entrySet()) {
      cachedIds.add(entry.getKey());
    }
    return EResult.err(
        loc,
        "independentFinalizeImportArtifact: artifact "
            + id
            + " not in cache. Cached="
            + cachedIds);
  }

  private static EResult<ExporterAny> walkBody(
      ExprContext xx, QExprId loc, ArtifactBody body) {
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
        var exp = exporterAnyToArtifactExporter(xx, loc, v);
        if (exp.hasErrors()) {
          exp.drainInto(errors);
          yield errsOrEmpty(errors);
        }
        var exporter = exp.requireValue("ArtifactSupport::walkBody Body exporter");
        var res = exporter.artifactExporterImportFromData(xx, loc, Opt.none(), body.data());
        res.drainInto(errors);
        yield errors.isEmpty() ? res : EResult.errs(errors);
      }
      case ArtifactBodyExporter.SourceRef sr -> {
        var ref = lookupRef(xx, loc, sr.id());
        if (ref.hasErrors()) {
          ref.drainInto(errors);
          yield errsOrEmpty(errors);
        }
        var v = ref.requireValue("ArtifactSupport::walkBody SourceRef ref");
        var exp = exporterAnyToArtifactExporter(xx, loc, v);
        if (exp.hasErrors()) {
          exp.drainInto(errors);
          yield errsOrEmpty(errors);
        }
        var exporter = exp.requireValue("ArtifactSupport::walkBody SourceRef exporter");
        var res = exporter.artifactExporterImportFromData(xx, loc, Opt.none(), body.data());
        res.drainInto(errors);
        yield errors.isEmpty() ? res : EResult.errs(errors);
      }
    };
  }

  private static <T> EResult<T> errsOrEmpty(TList<ExprErr> errors) {
    return EResult.errs(errors);
  }

  private static <T> EResult<T> drainOk(T value, TList<ExprErr> errors) {
    return EResult.mixed(value, errors);
  }

  public static final class ArtifactIdHolder {
    private @Nullable ArtifactIdSource id;

    public @Nullable ArtifactIdSource id() {
      return id;
    }

    public void set(ArtifactIdSource id) {
      this.id = id;
    }
  }

  public record ImportIndependentResult(
      ExporterAny value, TList<Artifact> dependencies, Finalizer finalizer) {
    @FunctionalInterface
    public interface Finalizer {
      void run(ExprContext xx, QExprId loc);
    }
  }
}
