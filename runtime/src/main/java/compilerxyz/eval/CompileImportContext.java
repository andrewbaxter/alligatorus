package compilerxyz.eval;

import compilerxyz.gen.Binding;

import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.langhelp.GlobalContext;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import compilerxyz.util.TOrderedMap;
import compilerxyz.util.TSet;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public final class CompileImportContext {
  public @Nullable Binding builtinScopeRootBinding = null;

  public final TList<CompileId> compilePath;
  public final Counter defSetCounter = new Counter();
  public final Counter evalRootCounter = new Counter();
  public final TMap<ArtifactIdSource, ExporterAny> deserializedArtifacts = new TMap<>();
  public final GlobalContext gx;
  public final ImportStateId importState;
  public final TSet<DefSetId> installedDefSets = new TSet<>();

  public final SourceId sourceId;
  public final SourceSpecAbsolute currentSpec;
  public final TList<CompileId> localDependencies = new TList<>();

  public @Nullable DefinitionSetClassLoader classLoader = null;

  public final Map<Object, Class<?>> compositeCache = new HashMap<>();

  public CompileImportContext(
      GlobalContext gx,
      ImportStateId importState,
      SourceId sourceId,
      TList<CompileId> compilePath,
      SourceSpecAbsolute currentSpec) {
    this.gx = gx;
    this.importState = importState;
    this.sourceId = sourceId;
    this.compilePath = compilePath;
    this.currentSpec = currentSpec;
  }
}
