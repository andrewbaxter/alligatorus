package compilerxyz.langhelp;

import compilerxyz.eval.CompileImport;
import compilerxyz.eval.CompileRes;
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

  public static Result<CompileResult, TList<ExprErr>> compile(
      GlobalContext gx, SourceSpecAbsolute entry) {
    var lookupResult = gx.sources.sourcesGet(entry);
    if (lookupResult instanceof Result.Err<Sources.SourceLookup, String> err) {
      return Result.err(
          TList.of(
              new ExprErr(
                  new QExprId(new SourceId(0), 0),
                  TList.of(new ExprSubErr.Opaque(err.error())))));
    }
    var lookup = ((Result.Ok<Sources.SourceLookup, String>) lookupResult).value();
    var module = SourceModuleToModule.sourceModuleToModule(lookup.id(), lookup.module());
    var res =
        CompileImport.compileImportId(
            gx, new ImportStateId(0), module, new TList<>());

    var allErrors = new TList<ExprErr>();
    gx.compileResultsLock.lock();
    try {
      for (var entryRes : gx.compileResults.values()) {
        entryRes.lock.readLock().lock();
        try {
          var value = entryRes.value;
          if (value != null) {
            collectErrors(value, allErrors);
          }
        } finally {
          entryRes.lock.readLock().unlock();
        }
      }
    } finally {
      gx.compileResultsLock.unlock();
    }
    collectErrors(res, allErrors);
    if (!allErrors.isEmpty()) {
      return Result.err(allErrors);
    }
    return Result.ok(new CompileResult(res.logs(), gx.capturedBytecode));
  }

  private static void collectErrors(CompileRes r, TList<ExprErr> out) {
    if (r.result() instanceof CompileResResult.Err err) {
      out.addAll(err.errors());
    }
  }

  public record CompileResult(TList<LogEntry> logs, TOrderedMap<String, byte[]> bytecode) {}
}
