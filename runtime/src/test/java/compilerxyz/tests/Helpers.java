package compilerxyz.tests;

import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.Artifacts;
import compilerxyz.eval.CompileId;
import compilerxyz.eval.ImportStateId;
import compilerxyz.eval.SourceId;
import compilerxyz.langhelp.GlobalContext;
import compilerxyz.langhelp.Sources;
import compilerxyz.syntax.SourceModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Result;
import compilerxyz.util.TMap;
import compilerxyz.eval.DefinitionSetClassLoader;

public final class Helpers {
  private Helpers() {}

  public static GlobalContext newGx(TMap<String, SourceModule> modules) {
    return newGxWithArtifacts(modules).gx();
  }

  public static GxAndArtifacts newGxWithArtifacts(TMap<String, SourceModule> modules) {
    var sources = new TMap<String, Sources.SourceLookup>();
    long i = 0;
    for (var entry : modules.entrySet()) {
      sources.put(entry.getKey(), new Sources.SourceLookup(new SourceId(i), entry.getValue()));
      i++;
    }
    var artifacts = new TestArtifacts();
    var gx = GlobalContext.newContext(new TestSources(sources), artifacts);

    return new GxAndArtifacts(gx, artifacts);
  }

  public record GxAndArtifacts(GlobalContext gx, TestArtifacts artifacts) {}

  public static final class TestArtifacts implements Artifacts {
    public final TMap<ArtifactIdSource, ArtifactBody> data = new TMap<>();
    private long nextId = 0;

    @Override
    public synchronized Result<ArtifactBody, String> artifactsGet(ArtifactIdSource id) {
      var v = data.getOrNull(id);
      if (v == null) return Result.err("Artifact not found: " + id);
      return Result.ok(v);
    }

    @Override
    public synchronized ArtifactIdSource artifactsReserveId() {
      var id = nextId;
      nextId++;
      return new ArtifactIdSource(new CompileId(new ImportStateId(0), new SourceId(0)), id);
    }

    @Override
    public synchronized void artifactsSet(ArtifactIdSource id, ArtifactBody body) {
      data.put(id, body);
    }
  }

  public static final class TestSources implements Sources {
    private final TMap<String, SourceLookup> modules;

    public TestSources(TMap<String, SourceLookup> modules) {
      this.modules = modules;
    }

    @Override
    public Result<SourceLookup, String> sourcesGet(SourceSpecAbsolute spec) {
      return switch (spec) {
        case SourceSpecAbsolute.Virtual v -> {
          var found = modules.getOrNull(v.path());
          if (found == null) yield Result.err("Module not found: " + v.path());
          yield Result.ok(found);
        }
      };
    }
  }
}
