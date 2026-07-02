package compilerxyz.langhelp;

import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.Artifacts;
import compilerxyz.eval.CompileId;
import compilerxyz.eval.ImportStateId;
import compilerxyz.eval.SourceId;
import compilerxyz.safejson.Json;
import compilerxyz.syntax.SourceModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Result;
import compilerxyz.util.TMap;
import compilerxyz.util.TOrderedMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import compilerxyz.eval.ExprErr;

public final class Main {
  private Main() {}

  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("usage: source <name>");
      System.exit(1);
    }
    var path = args[0];
    SourceModule module;
    try (var reader =
        new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
      var sb = new StringBuilder();
      var buf = new char[4096];
      int read;
      while ((read = reader.read(buf)) != -1) {
        sb.append(buf, 0, read);
      }
      module = Json.deserialize(SourceModule.class, sb.toString());
    } catch (IOException e) {
      throw new AssertionError("failed to read stdin", e);
    }

    var sources = new TOrderedMap<String, Sources.SourceLookup>();
    sources.put(path, new Sources.SourceLookup(new SourceId(0), module));
    var gx =
        GlobalContext.newContext(
            new CliSources(sources), new CliArtifacts());

    var result = Compile.compile(gx, new SourceSpecAbsolute.Virtual(path));
    switch (result) {
      case Result.Ok<Compile.CompileResult, ?> ok -> {
        var out = ok.value();
        for (var log : out.logs()) {
          System.out.println(log.message());
        }
      }
      case Result.Err<?, ?> err -> {
        @SuppressWarnings("unchecked")
        var errs = (Iterable<ExprErr>) err.error();
        for (var e : errs) {
          System.err.println("error at " + e.loc() + ": " + e.errors());
        }
        System.exit(1);
      }
    }
  }

  public static final class CliSources implements Sources {
    private final TOrderedMap<String, SourceLookup> modules;

    public CliSources(TOrderedMap<String, SourceLookup> modules) {
      this.modules = modules;
    }

    @Override
    public Result<SourceLookup, String> sourcesGet(SourceSpecAbsolute spec) {
      return switch (spec) {
        case SourceSpecAbsolute.Virtual v -> {
          var found = modules.getOrNull(v.path());
          if (found == null) {
            yield Result.err("Unknown source: " + v.path());
          }
          yield Result.ok(found);
        }
      };
    }
  }

  public static final class CliArtifacts implements Artifacts {
    private final TMap<ArtifactIdSource, ArtifactBody> bodies = new TMap<>();
    private long nextId = 0;

    @Override
    public synchronized Result<ArtifactBody, String> artifactsGet(ArtifactIdSource id) {
      var body = bodies.getOrNull(id);
      if (body == null) {
        return Result.err("Unknown artifact: " + id);
      }
      return Result.ok(body);
    }

    @Override
    public synchronized ArtifactIdSource artifactsReserveId() {
      var index = nextId;
      nextId++;
      return new ArtifactIdSource(new CompileId(new ImportStateId(0), new SourceId(0)), index);
    }

    @Override
    public synchronized void artifactsSet(ArtifactIdSource id, ArtifactBody body) {
      bodies.put(id, body);
    }
  }
}
