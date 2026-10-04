package compilerxyz.main.eval;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.eval.value.source.SourceSpecValue;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.langhelp.Compiles;
import compilerxyz.main.langhelp.SourceModuleToModule;
import compilerxyz.main.langhelp.Sources;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.nio.file.Path;
import java.util.ArrayList;

public final class ImportRun {
    private ImportRun() {}

    public sealed interface SpecOutcome {
        record BundleOut(SourceSpecAbsolute.Remote base) implements SpecOutcome {}

        record ModuleOut(CompileId compileId, SourceSpecAbsolute abs, CompileRes res) implements SpecOutcome {}
    }

    public static EResult<SpecOutcome> compileOne(ExprContext xx, QExprId loc, SourceSpecValue specValue) {
        var ix = xx.vx.ix;
        Result<SourceSpecAbsolute, String> resolveRes =
                switch (specValue) {
                    case SourceSpecValue.Virtual v -> Result.ok(new SourceSpecAbsolute.Virtual(v.name()));
                    case SourceSpecValue.Remote r ->
                        Result.ok(new SourceSpecAbsolute.Remote(
                                r.remote().url(), r.remote().hash(), ""));
                    case SourceSpecValue.Local l -> resolveRelative(ix, l.path());
                };
        Result.Ok<SourceSpecAbsolute, String> resolveResOk;
        switch (resolveRes) {
            case Result.Ok<SourceSpecAbsolute, String> ok -> resolveResOk = ok;
            case Result.Err<SourceSpecAbsolute, String> err -> {
                return EResult.err(loc, err.error());
            }
        }
        var abs = resolveResOk.value();
        return compileAbsolute(xx, loc, abs);
    }

    public static Result<SourceSpecAbsolute, String> resolveRelative(CompileImportContext ix, String relPath) {
        return switch (ix.currentSpec) {
            case SourceSpecAbsolute.Virtual cv -> Result.err("relative path is not allowed from a virtual module");
            case SourceSpecAbsolute.Local cl -> {
                var dir = Path.of(cl.path()).getParent();
                var base = dir == null ? Path.of("") : dir;
                yield Result.ok(new SourceSpecAbsolute.Local(
                        base.resolve(relPath).normalize().toString()));
            }
            case SourceSpecAbsolute.Remote cr -> {
                if (cr.subpath().isEmpty()) {
                    yield Result.err("relative path is only allowed from a bundle, not a single remote file");
                }
                var subpath = cr.subpath();
                var slashIdx = subpath.lastIndexOf('/');
                var baseDir = slashIdx < 0 ? "" : subpath.substring(0, slashIdx);
                var rel = relPath;
                var segs = new ArrayList<String>();
                if (!baseDir.isEmpty()) {
                    for (var s : baseDir.split("/")) {
                        if (!s.isEmpty() && !s.equals(".")) segs.add(s);
                    }
                }
                for (var s : rel.split("/")) {
                    if (s.isEmpty() || s.equals(".")) continue;
                    if (s.equals("..")) {
                        if (segs.isEmpty()) {
                            yield Result.err("path escapes bundle root: " + rel);
                        }
                        segs.remove(segs.size() - 1);
                    } else {
                        segs.add(s);
                    }
                }
                var resolvedPath = String.join("/", segs);
                yield Result.ok(new SourceSpecAbsolute.Remote(cr.baseUrl(), cr.hash(), resolvedPath));
            }
        };
    }

    public static EResult<SpecOutcome> compileAbsolute(ExprContext xx, QExprId loc, SourceSpecAbsolute abs) {
        var ix = xx.vx.ix;
        var gx = ix.gx;
        var resolvedRes = gx.sources.sourcesGet(abs);
        Result.Ok<Sources.SourceResolved, String> resolvedResOk;
        switch (resolvedRes) {
            case Result.Ok<Sources.SourceResolved, String> ok -> resolvedResOk = ok;
            case Result.Err<Sources.SourceResolved, String> err -> {
                return EResult.err(loc, err.error());
            }
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
                var compileRes = gx.compiles.compilesGetOrCompute(compileId, () -> {
                    var module = SourceModuleToModule.sourceModuleToModule(m.id(), m.module());
                    var cir = CompileModule.compileModuleId(gx, ix.importState, module, newPath, abs);
                    return new Compiles.CompileProduct(cir.res(), m.local(), cir.localDeps(), cir.localData());
                });
                yield EResult.ok(new SpecOutcome.ModuleOut(compileId, abs, compileRes));
            }
        };
    }

    public static EResult<CodeValue> materialize(ExprContext xx, QExprId loc, CompileRes compileRes) {
        var erb = new Erb(loc);
        Artifact artifact;
        switch (compileRes.result()) {
            case CompileResResult.Ok okRes -> {
                if (okRes.artifact().optIsNone()) {
                    erb.errPush("Imported module yields no value to import");
                    return EResult.errs(erb.errors());
                }
                artifact = okRes.artifact().optUnwrap();
            }
            case CompileResResult.Err errRes -> {
                erb.errPush("Import failed");
                return EResult.errs(erb.errors());
            }
        }
        var exporterRes = erb.mustEr(ArtifactSupport.importArtifact(xx, loc, artifact));
        if (exporterRes.optIsNone()) return EResult.errs(erb.errors());
        var value = ArtifactSupport.importedAsValue(exporterRes.optUnwrap());
        var cv = new CodeValue(Code.none(loc), EvalOutcome.ofValue(value));
        var valueType = value.valueType();
        var pdRes = erb.mustEr(GenAspectPostDeserialize.get(xx, loc, valueType));
        if (pdRes.optIsNone()) return EResult.errs(erb.errors());
        var finalRes = erb.mustEr(pdRes.optUnwrap().aspectPostDeserializePostDeserialize(xx, loc, cv));
        if (finalRes.optIsNone()) return EResult.errs(erb.errors());
        return EResult.ok(finalRes.optUnwrap());
    }
}
