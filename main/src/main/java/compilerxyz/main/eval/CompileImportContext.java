package compilerxyz.main.eval;

import compilerxyz.main.artifact.Artifacts;
import compilerxyz.main.eval.builtin.FileHandle;
import compilerxyz.main.eval.dynamic.CompileImportContextDynamic;
import compilerxyz.main.eval.dynamic.CompileImportContextTarget;
import compilerxyz.main.eval.dynamic.DynPanic;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.langhelp.Compiles;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.main.langhelp.Sources;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TSet;

@BuiltinFull(uniqueId = 6534L)
public final class CompileImportContext {
    Opt<Binding> builtinScopeRootBinding = Opt.none();

    final TList<CompileId> compilePath;
    private final Counter defSetCounter = new Counter();
    final Counter evalRootCounter = new Counter();
    private final Counter uniqueCounter = new Counter();
    public final Artifacts artifacts;
    final ArtifactImportScope artifactImportScope = new ArtifactImportScope();
    final GlobalContext gx;
    public final ImportStateId importState;
    final TSet<DefSetId> installedDefSets = new TSet<>();

    public final SourceId sourceId;
    final SourceSpecAbsolute currentSpec;
    final TList<CompileId> localDependencies = new TList<>();
    final TList<Compiles.LocalMeta> localData = new TList<>();

    public final CompileImportContextTarget dynamic;

    CompileImportContext(
            GlobalContext gx,
            ImportStateId importState,
            SourceId sourceId,
            TList<CompileId> compilePath,
            SourceSpecAbsolute currentSpec) {
        this.gx = gx;
        this.artifacts = gx.artifacts;
        this.importState = importState;
        this.sourceId = sourceId;
        this.compilePath = compilePath;
        this.currentSpec = currentSpec;
        this.dynamic = new CompileImportContextDynamic(gx);
    }

    public CompileId compileId() {
        return new CompileId(importState, sourceId);
    }

    public DefSetId defSetIdNext() {
        return new DefSetId(compileId(), defSetCounter.next());
    }

    public Unique uniqueNext() {
        return new Unique.Source(compileId(), uniqueCounter.next());
    }

    public FileHandle importFile(SourceSpecAbsolute abs) {
        Sources.SourceData data;
        switch (gx.sources.sourcesGetData(abs)) {
            case Result.Ok<Sources.SourceData, String> ok -> data = ok.value();
            case Result.Err<Sources.SourceData, String> err -> throw new DynPanic("import: " + err.error());
        }
        if (data.local().optIsSome()) localData.add(data.local().optUnwrap());
        return new FileHandle(data.bytes());
    }
}
