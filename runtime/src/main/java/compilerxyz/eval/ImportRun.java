package compilerxyz.eval;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.value.record.Record;
import compilerxyz.eval.value.record.RecordBaseDynamic;
import compilerxyz.eval.value.record.RecordField;
import compilerxyz.eval.value.source.SourceSpecValue;
import compilerxyz.eval.value.source.TypeBundle;
import compilerxyz.eval.value.source.TypeSourceSpec;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectRecord;
import compilerxyz.gen.Value;
import compilerxyz.langhelp.Compiles;
import compilerxyz.langhelp.SourceModuleToModule;
import compilerxyz.langhelp.Sources;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class ImportRun {
  private ImportRun() {}

  private sealed interface SpecOutcome {
    record BundleOut(SourceSpecAbsolute.Remote base) implements SpecOutcome {}

    record ModuleOut(CompileId compileId, SourceSpecAbsolute abs, CompileRes res)
        implements SpecOutcome {}
  }

  public static EResult<CodeValue> importRecord(ExprContext xx, QExprId loc, CodeValue argument) {
    if (!(argument.value() instanceof Opt.Some<Value> argSome)) {
      return EResult.err(loc, "import: argument must have a compile-time value");
    }
    var argValue = argSome.value();
    var recordAspectOpt = GenAspectRecord.getMaybe(xx, argValue.valueType());
    if (!(recordAspectOpt instanceof Opt.Some<compilerxyz.gen.AspectRecord> recordSome)) {
      return EResult.err(loc, "import: argument must be a record of source specs");
    }
    var takenFields = recordSome.value().aspectRecordTakeFields(xx, loc, argValue);

    var erb = new Erb(loc);
    var specs = new TOrderedMap<ConstKey, SourceSpecValue>();
    for (var entry : takenFields.entrySet()) {
      var key = entry.getKey();
      var fieldRes = entry.getValue();
      if (fieldRes.hasErrors()) {
        fieldRes.drainInto(erb.errors());
        continue;
      }
      var fieldCv = fieldRes.requireValue("import field");
      if (!(fieldCv.value() instanceof Opt.Some<Value> fieldSome)
          || !(fieldSome.value() instanceof ValueObjectConst voc)
          || !(voc.type() instanceof TypeSourceSpec)
          || !(voc.data() instanceof SourceSpecValue specValue)) {
        erb.errPush("import: entry '" + key.toCanonical() + "' is not a source spec");
        continue;
      }
      specs.put(key, specValue);
    }
    if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());

    var keys = new TList<ConstKey>(specs.size());
    keys.addAll(specs.keySet());
    var outcomes = compileAll(xx, loc, keys, specs);

    var ix = xx.vx.ix;
    var fields = new TOrderedMap<ConstKey, RecordField>();
    for (var i = 0; i < keys.size(); i++) {
      var key = keys.get(i);
      var outcomeRes = outcomes.get(i);
      if (outcomeRes.hasErrors()) {
        outcomeRes.drainInto(erb.errors());
        continue;
      }
      var outcome = outcomeRes.requireValue("import outcome");
      switch (outcome) {
        case SpecOutcome.BundleOut b -> {
          var bundleValue = ValueObjectConst.ofType(new TypeBundle(b.base()));
          fields.put(
              key,
              new RecordField.EvalRes(
                  EResult.ok(new CodeValue(Code.none(loc), EvalOutcome.ofValue(bundleValue)))));
        }
        case SpecOutcome.ModuleOut m -> {
          if (m.abs() instanceof SourceSpecAbsolute.Local) {
            ix.localDependencies.add(m.compileId());
          }
          var valueRes = materialize(xx, loc, m.res());
          if (valueRes.hasErrors()) {
            valueRes.drainInto(erb.errors());
            continue;
          }
          fields.put(key, new RecordField.EvalRes(EResult.ok(valueRes.requireValue("import value"))));
        }
      }
    }
    if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());

    Value resultRecord = new Record(Opt.<RecordBaseDynamic>none(), fields);
    return EResult.ok(new CodeValue(Code.none(loc), EvalOutcome.ofValue(resultRecord)));
  }

  private static TList<EResult<SpecOutcome>> compileAll(
      ExprContext xx, QExprId loc, TList<ConstKey> keys, TOrderedMap<ConstKey, SourceSpecValue> specs) {
    var results = new TList<EResult<SpecOutcome>>(keys.size());
    if (keys.size() <= 1) {
      for (var key : keys) results.add(compileOne(xx, loc, specs.getOrNull(key)));
      return results;
    }
    var pool = Executors.newFixedThreadPool(Math.min(keys.size(), 16));
    try {
      var futures = new ArrayList<Future<EResult<SpecOutcome>>>(keys.size());
      for (var key : keys) {
        var spec = specs.getOrNull(key);
        futures.add(pool.submit(() -> compileOne(xx, loc, spec)));
      }
      for (var future : futures) {
        try {
          results.add(future.get());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException("import: interrupted", e);
        } catch (java.util.concurrent.ExecutionException e) {
          var cause = e.getCause();
          throw cause instanceof RuntimeException re ? re : new RuntimeException("import task failed", cause);
        }
      }
    } finally {
      pool.shutdown();
    }
    return results;
  }

  private static EResult<SpecOutcome> compileOne(ExprContext xx, QExprId loc, SourceSpecValue specValue) {
    var ix = xx.vx.ix;
    var resolveRes = resolve(specValue, ix.currentSpec);
    if (resolveRes instanceof Result.Err<SourceSpecAbsolute, String> err) {
      return EResult.err(loc, err.error());
    }
    var abs = ((Result.Ok<SourceSpecAbsolute, String>) resolveRes).value();
    return compileAbsolute(xx, loc, abs);
  }

  public static EResult<CodeValue> importAbsolute(
      ExprContext xx, QExprId loc, SourceSpecAbsolute abs) {
    var outcomeRes = compileAbsolute(xx, loc, abs);
    if (outcomeRes.hasErrors()) return outcomeRes.reerr();
    var outcome = outcomeRes.requireValue("importAbsolute");
    return switch (outcome) {
      case SpecOutcome.BundleOut b ->
          EResult.ok(
              new CodeValue(
                  Code.none(loc),
                  EvalOutcome.ofValue(ValueObjectConst.ofType(new TypeBundle(b.base())))));
      case SpecOutcome.ModuleOut m -> {
        if (m.abs() instanceof SourceSpecAbsolute.Local) {
          xx.vx.ix.localDependencies.add(m.compileId());
        }
        yield materialize(xx, loc, m.res());
      }
    };
  }

  private static EResult<SpecOutcome> compileAbsolute(
      ExprContext xx, QExprId loc, SourceSpecAbsolute abs) {
    var ix = xx.vx.ix;
    var gx = ix.gx;
    var resolvedRes = gx.sources.sourcesGet(abs);
    if (resolvedRes instanceof Result.Err<Sources.SourceResolved, String> err) {
      return EResult.err(loc, err.error());
    }
    var resolved = ((Result.Ok<Sources.SourceResolved, String>) resolvedRes).value();
    return switch (resolved) {
      case Sources.SourceResolved.Bundle b -> EResult.ok(new SpecOutcome.BundleOut(b.base()));
      case Sources.SourceResolved.Module m -> {
        var compileId = new CompileId(ix.importState, m.id());
        if (ix.compilePath.contains(compileId)) {
          yield EResult.err(loc, "Import cycle detected");
        }
        var newPath = new TList<CompileId>(ix.compilePath.size());
        newPath.addAll(ix.compilePath);
        var compileRes =
            gx.compiles.compilesGetOrCompute(
                compileId,
                () -> {
                  var module = SourceModuleToModule.sourceModuleToModule(m.id(), m.module());
                  var cir =
                      CompileImport.compileImportId(gx, ix.importState, module, newPath, abs);
                  return new Compiles.CompileProduct(cir.res(), m.local(), cir.localDeps());
                });
        yield EResult.ok(new SpecOutcome.ModuleOut(compileId, abs, compileRes));
      }
    };
  }

  public static EResult<CodeValue> materialize(ExprContext xx, QExprId loc, CompileRes compileRes) {
    var erb = new Erb(loc);
    Artifact artifact;
    if (compileRes.result() instanceof CompileResResult.Ok okRes) {
      artifact = okRes.artifact();
    } else {
      var errRes = (CompileResResult.Err) compileRes.result();
      for (var ee : errRes.errors()) erb.errors().add(ee);
      erb.errPush("Import failed");
      return EResult.errs(erb.errors());
    }
    var exporterRes = erb.mustEr(ArtifactSupport.importArtifact(xx, loc, artifact));
    if (exporterRes.isEmpty()) return EResult.errs(erb.errors());
    var valueRes = erb.mustEr(exporterRes.get().exporterAnyAsValue());
    if (valueRes.isEmpty()) return EResult.errs(erb.errors());
    var cv = new CodeValue(Code.none(loc), EvalOutcome.ofValue(valueRes.get()));
    var valueType = valueRes.get().valueType();
    var pdRes = erb.mustEr(GenAspectPostDeserialize.get(xx, loc, valueType));
    if (pdRes.isEmpty()) return EResult.errs(erb.errors());
    var finalRes = erb.mustEr(pdRes.get().aspectPostDeserializePostDeserialize(xx, loc, cv));
    if (finalRes.isEmpty()) return EResult.errs(erb.errors());
    return EResult.ok(finalRes.get());
  }

  public static Result<SourceSpecAbsolute, String> resolve(
      SourceSpecValue spec, SourceSpecAbsolute current) {
    return switch (spec) {
      case SourceSpecValue.Virtual v -> Result.ok(new SourceSpecAbsolute.Virtual(v.name()));
      case SourceSpecValue.Remote r ->
          Result.ok(
              new SourceSpecAbsolute.Remote(r.remote().url(), r.remote().hash(), ""));
      case SourceSpecValue.Local l ->
          switch (current) {
            case SourceSpecAbsolute.Virtual cv ->
                Result.err("local import is not allowed from a virtual module");
            case SourceSpecAbsolute.Local cl -> {
              var dir = Path.of(cl.path()).getParent();
              var base = dir == null ? Path.of("") : dir;
              yield Result.ok(
                  new SourceSpecAbsolute.Local(base.resolve(l.path()).normalize().toString()));
            }
            case SourceSpecAbsolute.Remote cr -> {
              if (cr.subpath().isEmpty()) {
                yield Result.err(
                    "relative import is only allowed from a bundle, not a single remote file");
              }
              var resolved = posixResolve(posixDir(cr.subpath()), l.path());
              if (resolved instanceof Result.Err<String, String> err) {
                yield Result.err(err.error());
              }
              yield Result.ok(
                  new SourceSpecAbsolute.Remote(
                      cr.baseUrl(),
                      cr.hash(),
                      ((Result.Ok<String, String>) resolved).value()));
            }
          };
    };
  }

  private static String posixDir(String file) {
    var i = file.lastIndexOf('/');
    return i < 0 ? "" : file.substring(0, i);
  }

  static Result<String, String> posixResolve(String baseDir, String rel) {
    var segs = new ArrayList<String>();
    if (!baseDir.isEmpty()) {
      for (var s : baseDir.split("/")) {
        if (!s.isEmpty() && !s.equals(".")) segs.add(s);
      }
    }
    for (var s : rel.split("/")) {
      if (s.isEmpty() || s.equals(".")) continue;
      if (s.equals("..")) {
        if (segs.isEmpty()) return Result.err("path escapes bundle root: " + rel);
        segs.remove(segs.size() - 1);
      } else {
        segs.add(s);
      }
    }
    return Result.ok(String.join("/", segs));
  }
}
