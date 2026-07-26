package compilerxyz.tests;

import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.Artifacts;
import compilerxyz.eval.CompileId;
import compilerxyz.eval.ImportStateId;
import compilerxyz.eval.SourceId;
import compilerxyz.artifact.ArtifactBodyExporter;
import compilerxyz.langhelp.GlobalContext;
import compilerxyz.langhelp.Sources;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueArray;
import compilerxyz.safejson.JsonValueBoolean;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.safejson.JsonValueNumber;
import compilerxyz.safejson.JsonValueObject;
import compilerxyz.safejson.JsonValueString;
import compilerxyz.syntax.SourceModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Pair;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import compilerxyz.util.TOrderedMap;
import compilerxyz.eval.DefinitionSetClassLoader;

public final class Helpers {
  private Helpers() {}

  public static ArtifactBody artifactByTag(TestArtifacts artifacts, String tag) {
    ArtifactBody found = null;
    for (var entry : artifacts.data.entrySet()) {
      var body = entry.getValue();
      var matches =
          switch (body.exporter()) {
            case ArtifactBodyExporter.Body b ->
                switch (b.inner().data()) {
                  case JsonValueString s -> s.value().equals(tag);
                  case JsonValueNull ignored -> false;
                  case JsonValueBoolean ignored -> false;
                  case JsonValueNumber ignored -> false;
                  case JsonValueArray ignored -> false;
                  case JsonValueObject ignored -> false;
                };
            case ArtifactBodyExporter.Root ignored -> false;
            case ArtifactBodyExporter.SourceRef ignored -> false;
          };
      if (matches) {
        if (found != null) {
          throw new AssertionError("multiple artifacts with exporter tag " + tag);
        }
        found = body;
      }
    }
    if (found == null) {
      throw new AssertionError("no artifact with exporter tag " + tag);
    }
    return found;
  }

  public static JsonValue jsonRedacted(JsonValue value) {
    return switch (value) {
      case JsonValueObject o -> {
        var out = new TOrderedMap<String, JsonValue>();
        for (var entry : o.entries().entrySet()) {
          var k = entry.getKey();
          var v = entry.getValue();
          var redacted =
              switch (v) {
                case JsonValueString ignored ->
                    k.equals("compiled_bytecode") ? jsonStr("BYTECODE") : v;
                case JsonValueNumber ignored -> v;
                case JsonValueNull ignored -> v;
                case JsonValueBoolean ignored -> v;
                case JsonValueArray ignored -> jsonRedacted(v);
                case JsonValueObject inner ->
                    k.equals("key") && inner.entries().getOrNull("id") != null
                        ? jsonObj(jsonField("id", jsonStr("KEY")))
                        : jsonRedacted(v);
              };
          out.put(k, redacted);
        }
        yield new JsonValueObject(out);
      }
      case JsonValueArray a -> new JsonValueArray(a.elements().map(Helpers::jsonRedacted));
      case JsonValueNull n -> n;
      case JsonValueBoolean b -> b;
      case JsonValueNumber n -> n;
      case JsonValueString s -> s;
    };
  }

  @SafeVarargs
  public static JsonValueObject jsonObj(Pair<String, JsonValue>... entries) {
    return new JsonValueObject(TOrderedMap.ofEntries(entries));
  }

  public static JsonValueArray jsonArr(JsonValue... elements) {
    return new JsonValueArray(TList.of(elements));
  }

  public static Pair<String, JsonValue> jsonField(String key, JsonValue value) {
    return new Pair<>(key, value);
  }

  public static JsonValueString jsonStr(String value) {
    return new JsonValueString(value);
  }

  public static JsonValueNumber jsonNum(double value) {
    return new JsonValueNumber(value);
  }

  public static JsonValueNull jsonNull() {
    return new JsonValueNull();
  }

  public static compilerxyz.langhelp.Compile.CompileResult compileCaptured(
      TMap<String, SourceModule> modules) {
    var gx = newGx(modules);
    gx.captureBytecode = true;
    var res =
        compilerxyz.langhelp.Compile.compile(
            gx, new compilerxyz.syntax.SourceSpecAbsolute.Virtual("main"));
    if (!(res
        instanceof
        compilerxyz.util.Result.Ok<
                compilerxyz.langhelp.Compile.CompileResult,
                compilerxyz.util.TList<compilerxyz.eval.ExprErr>>
            ok)) {
      throw new AssertionError("compile failed: " + res);
    }
    return ok.value();
  }

  public static String disassembled(
      compilerxyz.langhelp.Compile.CompileResult result, String binaryName) {
    var bytes = result.bytecode().getOrNull(binaryName);
    if (bytes == null) {
      throw new AssertionError(
          "no captured bytecode for " + binaryName + "; captured: " + result.bytecode().keySet());
    }
    return disassemble(bytes);
  }

  public static String disassemble(byte[] bytecode) {
    var reader = new org.objectweb.asm.ClassReader(bytecode);
    var writer = new java.io.StringWriter();
    reader.accept(
        new org.objectweb.asm.util.TraceClassVisitor(
            null, new org.objectweb.asm.util.Textifier(), new java.io.PrintWriter(writer)),
        org.objectweb.asm.ClassReader.SKIP_DEBUG | org.objectweb.asm.ClassReader.SKIP_FRAMES);
    return writer.toString();
  }

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
