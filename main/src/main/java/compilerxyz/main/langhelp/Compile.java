package compilerxyz.main.langhelp;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.eval.CompileId;
import compilerxyz.main.eval.CompileModule;
import compilerxyz.main.eval.CompileResResult;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.ImportStateId;
import compilerxyz.main.eval.LogEntry;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;

public final class Compile {
    private Compile() {}

    public static Result<CompileResult, TList<ExprErr>> compile(
            GlobalContext gx, SourceSpecAbsolute entry, Sources.SourceResolved.Module lookup) {
        var rootImportState = new ImportStateId(0);
        var rootCompileId = new CompileId(rootImportState, lookup.id());
        gx.compiles.compilesPrepare(rootCompileId);
        var res = gx.compiles.compilesGetOrCompute(rootCompileId, entry, () -> {
            var module = SourceModuleToModule.sourceModuleToModule(lookup.id(), lookup.module());
            var cir = CompileModule.compileModuleId(gx, rootImportState, module, new TList<>(), entry);
            return new Compiles.CompileProduct(
                    cir.res(), lookup.local(), cir.localDeps(), cir.localData(), cir.exprMetas(), cir.scopeEvents());
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
        Opt<Artifact> artifact =
                switch (res.result()) {
                    case CompileResResult.Ok ok -> ok.artifact();
                    case CompileResResult.Err err ->
                        throw new IllegalStateException("root compile errors not collected");
                };
        return Result.ok(new CompileResult(res.logs(), gx.capturedBytecode, artifact));
    }

    public record CompileResult(TList<LogEntry> logs, TOrderedMap<String, byte[]> bytecode, Opt<Artifact> artifact) {}
}
