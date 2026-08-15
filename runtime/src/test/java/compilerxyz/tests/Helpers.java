package compilerxyz.tests;

import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.Artifacts;
import compilerxyz.eval.CompileId;
import compilerxyz.eval.CompileRes;
import compilerxyz.eval.ImportStateId;
import compilerxyz.eval.SourceId;
import compilerxyz.artifact.ArtifactBodyExporter;
import compilerxyz.langhelp.Compiles;
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
import compilerxyz.eval.ExprErr;
import compilerxyz.langhelp.Compile.CompileResult;
import compilerxyz.syntax.SourceSpecAbsolute.Virtual;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.locks.ReentrantLock;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;
import compilerxyz.langhelp.Compile;
import compilerxyz.util.Opt;
import java.util.function.Supplier;

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
                case JsonValueObject inner -> {
                  if (k.equals("artifact_external_key") && inner.entries().getOrNull("id") != null) {
                    yield jsonObj(jsonField("id", jsonStr("KEY")));
                  }
                  var some = inner.entries().getOrNull("some");
                  boolean someIsString =
                      some != null
                          && switch (some) {
                            case JsonValueString ignored2 -> true;
                            case JsonValueNumber ignored2 -> false;
                            case JsonValueNull ignored2 -> false;
                            case JsonValueBoolean ignored2 -> false;
                            case JsonValueArray ignored2 -> false;
                            case JsonValueObject ignored2 -> false;
                          };
                  if (k.equals("compiled_bytecode") && someIsString) {
                    yield jsonObj(jsonField("some", jsonStr("BYTECODE")));
                  }
                  yield jsonRedacted(v);
                }
              };
          out.put(k, redacted);
        }
        yield new JsonValueObject(out);
      }
      case JsonValueArray a -> {
        var _out = new TList<JsonValue>(a.elements().size());
        for (var _e : a.elements()) _out.add(Helpers.jsonRedacted(_e));
        yield new JsonValueArray(_out);
      }
      case JsonValueNull n -> n;
      case JsonValueBoolean b -> b;
      case JsonValueNumber n -> n;
      case JsonValueString s -> s;
    };
  }

  @SafeVarargs
  public static JsonValueObject jsonObj(Pair<String, JsonValue>... entries) {
    var m = new TOrderedMap<String, JsonValue>(entries.length);
    for (var e : entries) m.put(e.first(), e.second());
    return new JsonValueObject(m);
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

  public static CompileResult compileCaptured(
      TMap<String, SourceModule> modules) {
    var gx = newGx(modules);
    gx.captureBytecode = true;
    var res =
        Compile.compile(
            gx, new Virtual("main"));
    Result.Ok<CompileResult, TList<ExprErr>> ok;
    switch (res) {
      case Result.Ok<CompileResult, TList<ExprErr>> o -> ok = o;
      case Result.Err<CompileResult, TList<ExprErr>> e -> throw new AssertionError("compile failed: " + res);
    }
    return ok.value();
  }

  public static String disassembled(
      CompileResult result, String binaryName) {
    var bytes = result.bytecode().getOrNull(binaryName);
    if (bytes == null) {
      throw new AssertionError(
          "no captured bytecode for " + binaryName + "; captured: " + result.bytecode().keySet());
    }
    var _reader = new ClassReader(bytes);
    var _writer = new StringWriter();
    _reader.accept(
        new TraceClassVisitor(null, new Textifier(), new PrintWriter(_writer)),
        ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    return _writer.toString();
  }

  public static GlobalContext newGx(TMap<String, SourceModule> modules) {
    return newGxWithArtifacts(modules).gx();
  }

  public static GxAndArtifacts newGxWithArtifacts(TMap<String, SourceModule> modules) {
    var sources = new TMap<String, Sources.SourceResolved.Module>();
    long i = 0;
    for (var entry : modules.entrySet()) {
      sources.put(
          entry.getKey(),
          new Sources.SourceResolved.Module(
              new SourceId(i), entry.getValue(), Opt.none()));
      i++;
    }
    var artifacts = new TestArtifacts();
    var gx = GlobalContext.newContext(new TestSources(sources), artifacts, new TestCompiles());

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

  public static final class TestCompiles implements Compiles {
    private final TMap<CompileId, Entry> entries = new TMap<>();
    private final ReentrantLock mapLock =
        new ReentrantLock();

    private static final class Entry {
      final ReentrantLock lock =
          new ReentrantLock();
      CompileRes value;
    }

    @Override
    public void compilesPrepare(CompileId root) {}

    @Override
    public CompileRes compilesGetOrCompute(
        CompileId id, Supplier<Compiles.CompileProduct> compute) {
      Entry e;
      mapLock.lock();
      try {
        e = entries.getOrNull(id);
        if (e == null) {
          e = new Entry();
          entries.put(id, e);
        }
      } finally {
        mapLock.unlock();
      }
      e.lock.lock();
      try {
        if (e.value != null) return e.value;
        var product = compute.get();
        e.value = product.res();
        return e.value;
      } finally {
        e.lock.unlock();
      }
    }

    @Override
    public TList<CompileRes> compilesResults() {
      mapLock.lock();
      try {
        var out = new TList<CompileRes>();
        for (var e : entries.values()) {
          if (e.value != null) out.add(e.value);
        }
        return out;
      } finally {
        mapLock.unlock();
      }
    }
  }

  public static final class TestSources implements Sources {
    private final TMap<String, SourceResolved.Module> modules;

    public TestSources(TMap<String, SourceResolved.Module> modules) {
      this.modules = modules;
    }

    @Override
    public Result<SourceResolved, String> sourcesGet(SourceSpecAbsolute spec) {
      return switch (spec) {
        case SourceSpecAbsolute.Virtual v -> {
          var found = modules.getOrNull(v.name());
          if (found == null) yield Result.err("Module not found: " + v.name());
          yield Result.ok(found);
        }
        case SourceSpecAbsolute.Local l ->
            Result.err("test sources do not support local imports: " + l.path());
        case SourceSpecAbsolute.Remote r ->
            Result.err("test sources do not support remote imports: " + r.baseUrl());
      };
    }
  }
}
