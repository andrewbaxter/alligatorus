package compilerxyz.main.tests;

import com.google.gson.stream.JsonWriter;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactBodyExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.Artifacts;
import compilerxyz.main.eval.CompileId;
import compilerxyz.main.eval.CompileRes;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.SourceId;
import compilerxyz.main.gen.codec.SourceModuleCodec;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.langhelp.Compile.CompileResult;
import compilerxyz.main.langhelp.Compiles;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.main.langhelp.Sources;
import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueArray;
import compilerxyz.main.safejson.JsonValueBoolean;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.main.safejson.JsonValueNumber;
import compilerxyz.main.safejson.JsonValueNumberInt;
import compilerxyz.main.safejson.JsonValueObject;
import compilerxyz.main.safejson.JsonValueString;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.main.syntax.SourceSpecAbsolute.Virtual;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Pair;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import compilerxyz.prelude.util.TOrderedMap;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

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
                                    k.equals("compiled_bytecode") || k.equals("bytecode") ? jsonStr("BYTECODE") : v;
                                case JsonValueNumber ignored -> v;
                                case JsonValueNull ignored -> v;
                                case JsonValueBoolean ignored -> v;
                                case JsonValueArray ignored -> k.equals("locs") ? jsonStr("LOCS") : jsonRedacted(v);
                                case JsonValueObject inner -> {
                                    var someOpt = inner.entries().getOpt("some");
                                    boolean someIsString = someOpt.optIsSome()
                                            && switch (someOpt.optUnwrap()) {
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
                var out = new TList<JsonValue>(a.elements().size());
                for (var e : a.elements()) out.add(Helpers.jsonRedacted(e));
                yield new JsonValueArray(out);
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

    public static JsonValueNumberInt jsonNum(long value) {
        return new JsonValueNumberInt(value);
    }

    public static Result<CompileResult, TList<ExprErr>> compile(GlobalContext gx, SourceSpecAbsolute entry) {
        var resolved =
                switch (gx.sources.sourcesGet(entry)) {
                    case Result.Err<Sources.SourceResolved, String> err ->
                        throw new AssertionError("test entry does not resolve: " + err.error());
                    case Result.Ok<Sources.SourceResolved, String> ok -> ok.value();
                };
        return switch (resolved) {
            case Sources.SourceResolved.Module m -> Compile.compile(gx, entry, m);
            case Sources.SourceResolved.Bundle b -> throw new AssertionError("test entry is a bundle: " + entry);
            case Sources.SourceResolved.File f -> throw new AssertionError("test entry is not a module: " + entry);
        };
    }

    public static CompileResult compileCaptured(TMap<String, SourceModule> modules) {
        var gx = newGx(modules);
        gx.captureBytecode = true;
        var res = compile(gx, new Virtual("main"));
        Result.Ok<CompileResult, TList<ExprErr>> ok;
        switch (res) {
            case Result.Ok<CompileResult, TList<ExprErr>> o -> ok = o;
            case Result.Err<CompileResult, TList<ExprErr>> e -> throw new AssertionError("compile failed: " + res);
        }
        return ok.value();
    }

    public static String disassembled(CompileResult result, String binaryName) {
        var bytesOpt = result.bytecode().getOpt(binaryName);
        if (bytesOpt.optIsNone()) {
            throw new AssertionError("no captured bytecode for " + binaryName + "; captured: "
                    + result.bytecode().keySet());
        }
        var bytes = bytesOpt.optUnwrap();
        var reader = new ClassReader(bytes);
        var writer = new StringWriter();
        reader.accept(
                new TraceClassVisitor(null, new Textifier(), new PrintWriter(writer)),
                ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return writer.toString();
    }

    public static void writeModule(Path file, TList<SourceExpr> exprs) throws IOException {
        var sw = new StringWriter();
        var writer = new JsonWriter(sw);
        writer.setSerializeNulls(true);
        writer.setIndent("  ");
        SourceModuleCodec.encode(writer, new SourceModule.V1(exprs));
        writer.close();
        Files.writeString(file, sw.toString());
    }

    public static Compile.CompileResult compileOk(Path entry, Path cacheDir) {
        try (var cache = new CacheReal(cacheDir)) {
            var res = compile(
                    GlobalContext.newContext(cache, cache, cache), new SourceSpecAbsolute.Local(entry.toString()));
            Result.Ok<Compile.CompileResult, ?> ok;
            switch (res) {
                case Result.Ok<Compile.CompileResult, ?> o -> ok = o;
                case Result.Err<Compile.CompileResult, ?> err -> throw new AssertionError("compile failed: " + res);
            }
            return ok.value();
        }
    }

    public static GlobalContext newGx(TMap<String, SourceModule> modules) {
        return newGxWithArtifacts(modules).gx();
    }

    public static GxAndArtifacts newGxWithArtifacts(TMap<String, SourceModule> modules) {
        var sources = new TMap<String, Sources.SourceResolved.Module>();
        long i = 0;
        for (var entry : modules.entrySet()) {
            sources.put(
                    entry.getKey(), new Sources.SourceResolved.Module(new SourceId(i), entry.getValue(), Opt.none()));
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
        public synchronized Opt<ArtifactBody> artifactsGet(ArtifactIdSource id) {
            return data.getOpt(id);
        }

        @Override
        public synchronized ArtifactIdSource artifactsReserveId(CompileId compileId) {
            var id = nextId;
            nextId++;
            return new ArtifactIdSource(compileId, id);
        }

        @Override
        public synchronized void artifactsSet(ArtifactIdSource id, ArtifactBody body) {
            data.put(id, body);
        }
    }

    public static final class TestCompiles implements Compiles {
        private final TMap<CompileId, Entry> entries = new TMap<>();
        private final ReentrantLock mapLock = new ReentrantLock();

        private static final class Entry {
            final ReentrantLock lock = new ReentrantLock();
            Opt<CompileRes> value = Opt.none();
        }

        @Override
        public void compilesPrepare(CompileId root) {}

        @Override
        public CompileRes compilesGetOrCompute(
                CompileId id, SourceSpecAbsolute spec, Supplier<Compiles.CompileProduct> compute) {
            Entry e;
            mapLock.lock();
            try {
                e = entries.computeIfAbsentT(id, k -> new Entry());
            } finally {
                mapLock.unlock();
            }
            e.lock.lock();
            try {
                if (e.value.optIsSome()) return e.value.optUnwrap();
                var product = compute.get();
                e.value = Opt.some(product.res());
                return product.res();
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
                    if (e.value.optIsSome()) out.add(e.value.optUnwrap());
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
                    var foundOpt = modules.getOpt(v.name());
                    if (foundOpt.optIsNone()) yield Result.err("Module not found: " + v.name());
                    var found = foundOpt.optUnwrap();
                    yield Result.ok(found);
                }
                case SourceSpecAbsolute.Local l -> Result.err("test sources do not support local imports: " + l.path());
                case SourceSpecAbsolute.Remote r ->
                    Result.err("test sources do not support remote imports: "
                            + r.origin().remoteOriginDesc());
            };
        }

        @Override
        public Result<SourceData, String> sourcesGetData(SourceSpecAbsolute spec) {
            return Result.err("test sources do not support data files: " + spec);
        }
    }
}
