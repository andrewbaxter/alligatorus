package compilerxyz.eval;

import compilerxyz.util.TList;
import compilerxyz.util.TMap;

public final class EvalContext {
  public final TMap<DefSetId, DefinitionSet> definitionSets = new TMap<>();
  public final TList<ExternalEntry> externals = new TList<>();
  public final CompileImportContext ix;
  public final Counter nextLocalId = new Counter();

  public final Counter uniqueCounter = new Counter();

  public final TList<LogEntry> logs = new TList<>();

  public EvalContext(CompileImportContext ix) {
    this.ix = ix;
  }

  public record ExternalEntry(BindingId id, External external) {}
}
