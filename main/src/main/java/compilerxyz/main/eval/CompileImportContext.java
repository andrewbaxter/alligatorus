package compilerxyz.main.eval;

import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.langhelp.Compiles;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import compilerxyz.prelude.util.TSet;
import java.util.HashMap;
import java.util.Map;

public final class CompileImportContext {
    public Opt<Binding> builtinScopeRootBinding = Opt.none();

    public final TList<CompileId> compilePath;
    public final Counter defSetCounter = new Counter();
    public final Counter evalRootCounter = new Counter();
    public final Counter uniqueCounter = new Counter();
    public final TMap<ArtifactIdSource, Object> deserializedArtifacts = new TMap<>();
    public final GlobalContext gx;
    public final ImportStateId importState;
    public final TSet<DefSetId> installedDefSets = new TSet<>();

    public final SourceId sourceId;
    public final SourceSpecAbsolute currentSpec;
    public final TList<CompileId> localDependencies = new TList<>();
    public final TList<Compiles.LocalMeta> localData = new TList<>();

    public Opt<DefinitionSetClassLoader> classLoader = Opt.none();

    public final Map<String, Class<?>> compositeCache = new HashMap<>();

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
