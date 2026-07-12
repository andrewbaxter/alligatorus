package compilerxyz.eval;

import compilerxyz.gen.Binding;

import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.langhelp.GlobalContext;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import compilerxyz.util.TOrderedMap;
import compilerxyz.util.TSet;
import org.jspecify.annotations.Nullable;

public final class CompileImportContext {
  public @Nullable Binding builtinScopeRootBinding = null;

  public boolean builtinsRegistered = false;

  public final TOrderedMap<String, External> builtinDynamics = new TOrderedMap<>();
  public final TList<CompileId> compilePath;
  public final Counter defSetCounter = new Counter();
  public final TMap<ArtifactIdSource, ExporterAny> deserializedArtifacts = new TMap<>();
  public final GlobalContext gx;
  public final ImportStateId importState;
  public final TSet<DefSetId> installedDefSets = new TSet<>();

  public final SourceId sourceId;

  public CompileImportContext(
      GlobalContext gx,
      ImportStateId importState,
      SourceId sourceId,
      TList<CompileId> compilePath) {
    this.gx = gx;
    this.importState = importState;
    this.sourceId = sourceId;
    this.compilePath = compilePath;
  }
}
