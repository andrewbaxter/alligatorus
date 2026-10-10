package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.stream.JsonWriter;
import compilerxyz.main.gen.codec.SourceModuleCodec;
import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.main.langhelp.serve.ServeCompiler;
import compilerxyz.main.langhelp.serve.ServeProtocol;
import compilerxyz.main.langhelp.serve.ServeWatcher;
import compilerxyz.main.langserver.Announce;
import compilerxyz.main.langserver.AnnounceCompiled;
import compilerxyz.main.langserver.AnnounceDirty;
import compilerxyz.main.langserver.AnnounceRemoved;
import compilerxyz.main.langserver.AnnounceSettled;
import compilerxyz.main.langserver.ClientMessage;
import compilerxyz.main.langserver.ClientMessageCodec;
import compilerxyz.main.langserver.Compiled;
import compilerxyz.main.langserver.Request;
import compilerxyz.main.langserver.RequestExprMeta;
import compilerxyz.main.langserver.RequestFileState;
import compilerxyz.main.langserver.RequestFlush;
import compilerxyz.main.langserver.RequestMeta;
import compilerxyz.main.langserver.RequestPause;
import compilerxyz.main.langserver.RequestScopeMeta;
import compilerxyz.main.langserver.RequestSourceRead;
import compilerxyz.main.langserver.Response;
import compilerxyz.main.langserver.ResponseExprMeta;
import compilerxyz.main.langserver.ResponseFileState;
import compilerxyz.main.langserver.ResponseFlush;
import compilerxyz.main.langserver.ResponseMeta;
import compilerxyz.main.langserver.ResponsePause;
import compilerxyz.main.langserver.ResponseScopeMeta;
import compilerxyz.main.langserver.ResponseSourceRead;
import compilerxyz.main.langserver.ServerMessage;
import compilerxyz.main.langserver.ServerMessageCodec;
import compilerxyz.main.langserver.ServerMessageEv;
import compilerxyz.main.langserver.ServerMessageRes;
import compilerxyz.main.langserver.SourceLocal;
import compilerxyz.main.langserver.SourceRemote;
import compilerxyz.main.safejson.JsonValues;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0221ServeTest {
    private static void writeModule(Path file, TList<SourceExpr> exprs) throws IOException {
        var sw = new StringWriter();
        var writer = new JsonWriter(sw);
        writer.setSerializeNulls(true);
        writer.setIndent("");
        SourceModuleCodec.encode(writer, new SourceModule.V1(exprs));
        writer.close();
        Files.writeString(file, sw.toString());
    }

    private static final class Harness implements AutoCloseable {
        final PrintWriter toServer;
        final BufferedReader fromServer;
        final ServeCompiler compiler;
        final Thread thread;
        final CacheReal cache;
        final ServeWatcher watcher;
        long nextId = 1;

        Harness(Path dir, Path cacheDir) throws IOException {
            var stdin = new PipedInputStream(1 << 16);
            toServer = new PrintWriter(new PipedOutputStream(stdin), true, StandardCharsets.UTF_8);
            var stdoutIn = new PipedInputStream(1 << 20);
            var stdout = new PipedOutputStream(stdoutIn);
            fromServer = new BufferedReader(new InputStreamReader(stdoutIn, StandardCharsets.UTF_8));
            var protocol = new ServeProtocol(stdin, stdout);
            cache = new CacheReal(cacheDir);
            watcher = new ServeWatcher(dir);
            compiler = new ServeCompiler(dir, cache, watcher, protocol);
            var pump = new Thread(() -> {
                while (true) {
                    var changes = watcher.watcherDrain(true);
                    if (changes.isEmpty()) return;
                    compiler.compilerPost(new ServeCompiler.Job.Changes(changes));
                }
            });
            pump.setDaemon(true);
            pump.start();
            var reader = new Thread(() -> {
                while (true) {
                    var message = protocol.protocolRead();
                    if (message.optIsNone()) {
                        compiler.compilerPost(new ServeCompiler.Job.Stop());
                        return;
                    }
                    compiler.compilerPost(new ServeCompiler.Job.Message(message.optUnwrap()));
                }
            });
            reader.setDaemon(true);
            reader.start();
            thread = new Thread(compiler::compilerRun);
            thread.start();
        }

        ServerMessage read() throws IOException {
            var line = fromServer.readLine();
            if (line == null) throw new AssertionError("server output ended");
            return ServerMessageCodec.deserialize(line);
        }

        List<Announce> readUntil(Predicate<Announce> last) throws IOException {
            var out = new ArrayList<Announce>();
            while (true) {
                var message = read();
                switch (message) {
                    case ServerMessageEv ev -> {
                        out.add(ev.ev());
                        if (last.test(ev.ev())) return out;
                    }
                    case ServerMessageRes res -> throw new AssertionError("unexpected reply: " + res);
                }
            }
        }

        Response request(Request req) throws IOException {
            var id = nextId++;
            toServer.println(JsonValues.serialize(ClientMessageCodec.toValue(new ClientMessage(id, req))));
            while (true) {
                var message = read();
                switch (message) {
                    case ServerMessageEv ev -> throw new AssertionError("unexpected announcement: " + ev);
                    case ServerMessageRes res -> {
                        assertEquals(id, res.id());
                        return res.res();
                    }
                }
            }
        }

        List<Announce> requestAnnouncing(Request req, List<Response> reply) throws IOException {
            var id = nextId++;
            toServer.println(JsonValues.serialize(ClientMessageCodec.toValue(new ClientMessage(id, req))));
            var out = new ArrayList<Announce>();
            while (true) {
                var message = read();
                switch (message) {
                    case ServerMessageEv ev -> out.add(ev.ev());
                    case ServerMessageRes res -> {
                        assertEquals(id, res.id());
                        reply.add(res.res());
                        return out;
                    }
                }
            }
        }

        @Override
        public void close() throws InterruptedException {
            toServer.close();
            thread.join(10000);
            watcher.close();
            cache.close();
        }
    }

    private static boolean settled(Announce a, String path) {
        return switch (a) {
            case AnnounceSettled s -> s.path().equals(path);
            case AnnounceDirty d -> false;
            case AnnounceCompiled c -> false;
            case AnnounceRemoved r -> false;
        };
    }

    private static boolean dirty(Announce a, String path) {
        return switch (a) {
            case AnnounceDirty d -> d.path().equals(path);
            case AnnounceSettled s -> false;
            case AnnounceCompiled c -> false;
            case AnnounceRemoved r -> false;
        };
    }

    private static List<Compiled> compiledOf(List<Announce> announces, String path) {
        var out = new ArrayList<Compiled>();
        for (var a : announces) {
            switch (a) {
                case AnnounceCompiled c -> {
                    switch (c.value().source()) {
                        case SourceLocal l -> {
                            if (l.path().equals(path)) out.add(c.value());
                        }
                        case SourceRemote r -> {}
                    }
                }
                case AnnounceDirty d -> {}
                case AnnounceSettled s -> {}
                case AnnounceRemoved r -> {}
            }
        }
        return out;
    }

    private static Opt<String> sourceText(Response response) {
        return switch (response) {
            case ResponseSourceRead r -> r.text();
            default -> throw new AssertionError("expected a source read reply, got " + response);
        };
    }

    private static boolean has(List<Announce> announces, Announce wanted) {
        return announces.stream().anyMatch(a -> a.equals(wanted));
    }

    @Test
    public void test00100ServeLifecycle(@TempDir Path tmp) throws Exception {
        var mb = new TestMb();
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var a = src.resolve("a.at").toString();
        var b = src.resolve("b.at").toString();
        writeModule(Path.of(b), TList.of(mb.return_(mb.str("v1"))));
        writeModule(
                Path.of(a),
                TList.of(mb.call(
                        mb.log(),
                        mb.field(
                                mb.importRecord(TList.of(
                                        new SourceExpr.RecordEntry(mb.str("v"), mb.sourceLocal(mb.str("b.at"))))),
                                mb.str("v")))));

        try (var h = new Harness(src, tmp.resolve("cache"))) {
            var initial = h.readUntil(x -> settled(x, b));
            assertTrue(has(initial, new AnnounceDirty(a)) && has(initial, new AnnounceDirty(b)));
            assertEquals(1, compiledOf(initial, a).size());
            assertEquals(1, compiledOf(initial, b).size());
            assertEquals("v1", compiledOf(initial, a).get(0).logs().get(0).message());
            assertTrue(has(initial, new AnnounceSettled(a)));

            writeModule(Path.of(b), TList.of(mb.return_(mb.str("v2"))));
            var edited = h.readUntil(x -> settled(x, b));
            assertTrue(has(edited, new AnnounceDirty(a)) && has(edited, new AnnounceDirty(b)));
            assertEquals("v2", compiledOf(edited, a).get(0).logs().get(0).message());
            assertTrue(has(edited, new AnnounceSettled(a)));

            switch (h.request(new RequestFileState(a))) {
                case ResponseFileState s -> {
                    assertTrue(!s.value().dirty());
                    assertEquals(1, s.value().compiles().size());
                    assertTrue(s.value().compiles().get(0).errors().isEmpty());
                }
                default -> throw new AssertionError("expected a file state reply");
            }

            Files.delete(Path.of(b));
            var deleted = h.readUntil(x -> settled(x, a));
            assertTrue(has(deleted, new AnnounceRemoved(b)));
            var aCompiled = compiledOf(deleted, a).get(0);
            assertEquals(1, aCompiled.errors().size());
            var error = aCompiled.errors().get(0);
            assertTrue(error.location().expr().optIsSome());
            assertEquals(1, error.related().size());
            assertEquals(new SourceLocal(b), error.related().get(0).location().source());

            assertEquals(new ResponsePause(), h.request(new RequestPause(true)));
            writeModule(Path.of(b), TList.of(mb.return_(mb.str("v3"))));
            var paused = h.readUntil(x -> dirty(x, a));
            assertTrue(has(paused, new AnnounceDirty(b)) || has(paused, new AnnounceDirty(a)));
            var reply = new ArrayList<Response>();
            var flushed = h.requestAnnouncing(new RequestFlush(), reply);
            assertTrue(has(flushed, new AnnounceSettled(a)) && has(flushed, new AnnounceSettled(b)));
            assertEquals("v3", compiledOf(flushed, a).get(0).logs().get(0).message());
            switch (reply.get(0)) {
                case ResponseFlush f -> assertEquals(2, f.compiled().size());
                default -> throw new AssertionError("expected a flush reply, got " + reply.get(0));
            }

            writeModule(Path.of(b), TList.of(mb.return_(mb.str("v4"))));
            h.readUntil(x -> dirty(x, a));
            var unpaused = h.requestAnnouncing(new RequestPause(false), reply);
            assertEquals("v4", compiledOf(unpaused, a).get(0).logs().get(0).message());

            var local = sourceText(h.request(new RequestSourceRead(new SourceLocal(b))));
            assertTrue(local.optIsSome() && local.optUnwrap().contains("v4"));
            var remote = sourceText(h.request(new RequestSourceRead(
                    new SourceRemote("remote:git:https://example.invalid/x@" + "0".repeat(40) + "!m.at"))));
            assertTrue(remote.optIsNone());
        }
    }

    private static List<String> fieldNames(compilerxyz.main.langserver.Meta meta) {
        var out = new ArrayList<String>();
        for (var f : meta.fields()) out.add(f.name());
        return out;
    }

    @Test
    public void test00200MetaQueries(@TempDir Path tmp) throws Exception {
        var mb = new TestMb();
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var a = src.resolve("a.at").toString();
        var recordUse = mb.scopeUse("r");
        var fieldUse = mb.field(recordUse, mb.str("x"));
        writeModule(
                Path.of(a),
                TList.of(
                        mb.bind("s", mb.str("hello")),
                        mb.bind(
                                "r",
                                mb.recordOf(TList.of(
                                        new TestMb.RecordPair("x", mb.str("v")),
                                        new TestMb.RecordPair("y", mb.str("w"))))),
                        mb.call(mb.log(), fieldUse)));
        try (var h = new Harness(src, tmp.resolve("cache"))) {
            h.readUntil(x -> settled(x, a));
            long fieldX;
            switch (h.request(new RequestExprMeta(recordUse.id().value(), new SourceLocal(a)))) {
                case ResponseExprMeta r -> {
                    assertEquals(1, r.metas().size());
                    var meta = r.metas().get(0).meta();
                    assertEquals("record", meta.kind());
                    assertEquals(List.of("x", "y"), fieldNames(meta));
                    assertTrue(meta.fields().get(0).meta().optIsSome());
                    fieldX = meta.fields().get(0).meta().optUnwrap();
                }
                default -> throw new AssertionError("expected an expr meta reply");
            }
            switch (h.request(new RequestMeta(TList.of(fieldX, -1L)))) {
                case ResponseMeta r -> {
                    assertEquals(1, r.metas().size());
                    assertEquals(fieldX, r.metas().get(0).id());
                    assertEquals("string_const", r.metas().get(0).kind());
                }
                default -> throw new AssertionError("expected a meta reply");
            }
            switch (h.request(new RequestScopeMeta(fieldUse.id().value(), new SourceLocal(a)))) {
                case ResponseScopeMeta r -> {
                    assertEquals(1, r.scopes().size());
                    var names = new ArrayList<String>();
                    for (var e : r.scopes().get(0).entries()) names.add(e.name());
                    assertEquals(List.of("s", "r"), names);
                    var rEntry = r.scopes().get(0).entries().get(1);
                    assertTrue(rEntry.meta().optIsSome());
                    assertEquals(List.of("x", "y"), fieldNames(rEntry.meta().optUnwrap()));
                }
                default -> throw new AssertionError("expected a scope meta reply");
            }
        }
    }
}
