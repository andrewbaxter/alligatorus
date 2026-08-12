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
import compilerxyz.gen.AspectRecord;
import java.util.concurrent.ExecutionException;

public final class ImportRun {
  private ImportRun() {}

  private sealed interface SpecOutcome {
    record BundleOut(SourceSpecAbsolute.Remote base) implements SpecOutcome {}

    record ModuleOut(CompileId compileId, SourceSpecAbsolute abs, CompileRes res)
        implements SpecOutcome {}
  }

  public static EResult<CodeValue> importRecord(ExprContext xx, QExprId loc, CodeValue argument) {
    if (argument.value().optIsNone()) {
      return EResult.err(loc, "import: argument must have a compile-time value");
    }
    var argValue = argument.value().optUnwrap();
    var recordAspectOpt = GenAspectRecord.getMaybe(xx, argValue.valueType());
    if (recordAspectOpt.optIsNone()) {
      return EResult.err(loc, "import: argument must be a record of source specs");
    }
    var takenFields = recordAspectOpt.optUnwrap().aspectRecordTakeFields(xx, loc, argValue);

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
      if (fieldCv.value().optIsNone()
          || !(fieldCv.value().optUnwrap() instanceof ValueObjectConst voc)
          || !(voc.type() instanceof TypeSourceSpec)
          || !(voc.data() instanceof SourceSpecValue specValue)) {
        erb.errPush("import: entry '" + key.constKeyToCanonical() + "' is not a source spec");
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
          var bundleValue = new ValueObjectConst(compilerxyz.eval.value.object_.ValueObjectConst.plainTypeType(), new TypeBundle(b.base()));
          fields.put(
              key,
              new RecordField.EvalRes(
                  EResult.ok(new CodeValue(Code.none(loc), EvalOutcome.ofValue(bundleValue)))));
        }
        case SpecOutcome.ModuleOut m -> {
          switch (m.abs()) {
            case SourceSpecAbsolute.Local ignored -> ix.localDependencies.add(m.compileId());
            case SourceSpecAbsolute.Virtual v -> {}
            case SourceSpecAbsolute.Remote r -> {}
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
    return EResult.ok(new CodeValue(argument.code(), EvalOutcome.ofValue(resultRecord)));
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
        } catch (ExecutionException e) {
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
    Result.Ok<SourceSpecAbsolute, String> resolveResOk;
    switch (resolveRes) {
      case Result.Ok<SourceSpecAbsolute, String> ok -> resolveResOk = ok;
      case Result.Err<SourceSpecAbsolute, String> err -> { return EResult.err(loc, err.error()); }
    }
    var abs = resolveResOk.value();
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
                  EvalOutcome.ofValue(new ValueObjectConst(compilerxyz.eval.value.object_.ValueObjectConst.plainTypeType(), new TypeBundle(b.base())))));
      case SpecOutcome.ModuleOut m -> {
        switch (m.abs()) {
          case SourceSpecAbsolute.Local ignored -> xx.vx.ix.localDependencies.add(m.compileId());
          case SourceSpecAbsolute.Virtual v -> {}
          case SourceSpecAbsolute.Remote r -> {}
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
    Result.Ok<Sources.SourceResolved, String> resolvedResOk;
    switch (resolvedRes) {
      case Result.Ok<Sources.SourceResolved, String> ok -> resolvedResOk = ok;
      case Result.Err<Sources.SourceResolved, String> err -> { return EResult.err(loc, err.error()); }
    }
    var resolved = resolvedResOk.value();
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
    switch (compileRes.result()) {
      case CompileResResult.Ok okRes -> artifact = okRes.artifact();
      case CompileResResult.Err errRes -> {
        for (var ee : errRes.errors()) erb.errors().add(ee);
        erb.errPush("Import failed");
        return EResult.errs(erb.errors());
      }
    }
    var exporterRes = erb.mustEr(ArtifactSupport.importArtifact(xx, loc, artifact));
    if (exporterRes.optIsNone()) return EResult.errs(erb.errors());
    var valueRes = erb.mustEr(exporterRes.optUnwrap().exporterAnyAsValue());
    if (valueRes.optIsNone()) return EResult.errs(erb.errors());
    var cv = new CodeValue(Code.none(loc), EvalOutcome.ofValue(valueRes.optUnwrap()));
    var valueType = valueRes.optUnwrap().valueType();
    var pdRes = erb.mustEr(GenAspectPostDeserialize.get(xx, loc, valueType));
    if (pdRes.optIsNone()) return EResult.errs(erb.errors());
    var finalRes = erb.mustEr(pdRes.optUnwrap().aspectPostDeserializePostDeserialize(xx, loc, cv));
    if (finalRes.optIsNone()) return EResult.errs(erb.errors());
    return EResult.ok(finalRes.optUnwrap());
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
              var subpath = cr.subpath();
              var slashIdx = subpath.lastIndexOf('/');
              var resolved = posixResolve(slashIdx < 0 ? "" : subpath.substring(0, slashIdx), l.path());
              Result.Ok<String, String> resolvedOk;
              switch (resolved) {
                case Result.Ok<String, String> ok -> resolvedOk = ok;
                case Result.Err<String, String> err -> { yield Result.err(err.error()); }
              }
              yield Result.ok(
                  new SourceSpecAbsolute.Remote(
                      cr.baseUrl(),
                      cr.hash(),
                      resolvedOk.value()));
            }
          };
    };
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
