package compilerxyz.langhelp;

import compilerxyz.eval.CompileId;
import compilerxyz.eval.CompileImport;
import compilerxyz.eval.CompileResResult;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.ExprSubErr;
import compilerxyz.eval.ImportStateId;
import compilerxyz.eval.LogEntry;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.SourceId;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;

public final class Compile {
    private Compile() {}

    public static Result<CompileResult, TList<ExprErr>> compile(GlobalContext gx, SourceSpecAbsolute entry) {
        var lookupResult = gx.sources.sourcesGet(entry);
        Sources.SourceResolved resolvedRaw;
        switch (lookupResult) {
            case Result.Err<Sources.SourceResolved, String> err -> {
                return Result.err(TList.of(
                        new ExprErr(new QExprId(new SourceId(0), 0), TList.of(new ExprSubErr.Opaque(err.error())))));
            }
            case Result.Ok<Sources.SourceResolved, String> ok -> resolvedRaw = ok.value();
        }
        Sources.SourceResolved.Module lookup;
        switch (resolvedRaw) {
            case Sources.SourceResolved.Module m -> lookup = m;
            case Sources.SourceResolved.Bundle ignored -> {
                return Result.err(TList.of(new ExprErr(
                        new QExprId(new SourceId(0), 0),
                        TList.of(new ExprSubErr.Opaque("Entry point must be a source file, not a bundle")))));
            }
        }
        var rootImportState = new ImportStateId(0);
        var rootCompileId = new CompileId(rootImportState, lookup.id());
        gx.compiles.compilesPrepare(rootCompileId);
        var res = gx.compiles.compilesGetOrCompute(rootCompileId, () -> {
            var module = SourceModuleToModule.sourceModuleToModule(lookup.id(), lookup.module());
            var cir = CompileImport.compileImportId(gx, rootImportState, module, new TList<>(), entry);
            return new Compiles.CompileProduct(cir.res(), lookup.local(), cir.localDeps());
        });

        var allErrors = new TList<ExprErr>();
        for (var entryRes : gx.compiles.compilesResults()) {
            switch (entryRes.result()) {
                case CompileResResult.Err err -> allErrors.addAll(err.errors());
                case CompileResResult.Ok ignored -> {}
            }
        }
        if (!allErrors.isEmpty()) {
            return Result.err(allErrors);
        }
        return Result.ok(new CompileResult(res.logs(), gx.capturedBytecode));
    }

    public record CompileResult(TList<LogEntry> logs, TOrderedMap<String, byte[]> bytecode) {}
}
