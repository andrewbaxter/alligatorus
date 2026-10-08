package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.stream.JsonWriter;
import compilerxyz.main.eval.builtin.Zbase32;
import compilerxyz.main.gen.codec.SourceModuleCodec;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.main.safejson.Json;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0180ImportRealTest {

    private static String serializeModule(TList<SourceExpr> exprs) throws IOException {
        var sw = new StringWriter();
        var writer = new JsonWriter(sw);
        writer.setSerializeNulls(true);
        writer.setIndent("");
        SourceModuleCodec.encode(writer, new SourceModule.V1(exprs), Json.Style.COMPACT);
        writer.close();
        return sw.toString();
    }

    private static void writeModule(Path file, TList<SourceExpr> exprs) throws IOException {
        Files.writeString(file, serializeModule(exprs));
    }

    private static byte[] moduleBytes(TList<SourceExpr> exprs) throws IOException {
        return serializeModule(exprs).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] sha256(byte[] data) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(data);
    }

    private static GlobalContext gxFor(CacheReal cache) {
        return GlobalContext.newContext(cache, cache, cache);
    }

    private static Compile.CompileResult expectOk(Result<Compile.CompileResult, ?> res) {
        Result.Ok<Compile.CompileResult, ?> ok;
        switch (res) {
            case Result.Ok<Compile.CompileResult, ?> o -> ok = o;
            case Result.Err<Compile.CompileResult, ?> e -> throw new AssertionError("compile failed: " + res);
        }
        return ok.value();
    }

    private static SourceExpr importField(TestMb mb, String key, SourceExpr spec) {
        var entry = new SourceExpr.RecordEntry(mb.str(key), spec);
        return mb.field(mb.importRecord(TList.of(entry)), mb.str(key));
    }

    @Test
    public void test00000Zbase32RoundTrip() {
        var data = new byte[] {0, 1, 2, 3, (byte) 200, (byte) 255, 42, 7};
        var encoded = Zbase32.encode(data);
        assertTrue(Arrays.equals(data, Zbase32.decode(encoded)));
    }

    @Test
    public void test00100LocalImport(@TempDir Path tmp) throws IOException {
        var mb = new TestMb();
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve("other.at"), TList.of(mb.return_(mb.str("from-other"))));
        writeModule(
                src.resolve("main.at"),
                TList.of(mb.call(mb.log(), importField(mb, "v", mb.sourceLocal(mb.str("other.at"))))));

        try (var cache = new CacheReal(tmp.resolve("cache"))) {
            var res = Helpers.compile(
                    gxFor(cache),
                    new SourceSpecAbsolute.Local(src.resolve("main.at").toString()));
            var ok = expectOk(res);
            assertEquals(1, ok.logs().size());
            assertEquals("from-other", ok.logs().get(0).message());
        }
    }

    @Test
    public void test00200ParallelImports(@TempDir Path tmp) throws IOException {
        var mb = new TestMb();
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve("a.at"), TList.of(mb.return_(mb.str("AA"))));
        writeModule(src.resolve("b.at"), TList.of(mb.return_(mb.str("BB"))));
        var both = new SourceExpr.RecordEntry(mb.str("a"), mb.sourceLocal(mb.str("a.at")));
        var second = new SourceExpr.RecordEntry(mb.str("b"), mb.sourceLocal(mb.str("b.at")));
        var rec = mb.importRecord(TList.of(both, second));
        writeModule(
                src.resolve("main.at"),
                TList.of(mb.seqOf(
                        mb.call(mb.log(), mb.field(rec, mb.str("a"))), mb.call(mb.log(), mb.field(rec, mb.str("b"))))));

        try (var cache = new CacheReal(tmp.resolve("cache"))) {
            var ok = expectOk(Helpers.compile(
                    gxFor(cache),
                    new SourceSpecAbsolute.Local(src.resolve("main.at").toString())));
            assertEquals(2, ok.logs().size());
            assertEquals("AA", ok.logs().get(0).message());
            assertEquals("BB", ok.logs().get(1).message());
        }
    }

    @Test
    public void test00300CacheInvalidation(@TempDir Path tmp) throws IOException {
        var mb = new TestMb();
        var src = tmp.resolve("src");
        var cacheDir = tmp.resolve("cache");
        Files.createDirectories(src);
        var otherFile = src.resolve("other.at");
        var mainFile = src.resolve("main.at");
        writeModule(otherFile, TList.of(mb.return_(mb.str("v1"))));
        writeModule(mainFile, TList.of(mb.call(mb.log(), importField(mb, "v", mb.sourceLocal(mb.str("other.at"))))));
        var entry = new SourceSpecAbsolute.Local(mainFile.toString());

        try (var cache = new CacheReal(cacheDir)) {
            var ok = expectOk(Helpers.compile(gxFor(cache), entry));
            assertEquals("v1", ok.logs().get(0).message());
        }

        writeModule(otherFile, TList.of(mb.return_(mb.str("v2"))));
        Files.setLastModifiedTime(
                otherFile,
                FileTime.fromMillis(Files.getLastModifiedTime(otherFile).toMillis() + 100000));

        try (var cache = new CacheReal(cacheDir)) {
            var ok = expectOk(Helpers.compile(gxFor(cache), entry));
            assertEquals("v2", ok.logs().get(0).message());
        }
    }

    @Test
    public void test00350PersistentCacheAcrossContexts(@TempDir Path tmp) throws IOException {
        var mb = new TestMb();
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var cacheDir = tmp.resolve("cache");

        writeModule(src.resolve("one.at"), TList.of(mb.return_(mb.str("cached-one"))));
        var oneEntry = new SourceSpecAbsolute.Local(src.resolve("one.at").toString());
        try (var cache1 = new CacheReal(cacheDir)) {
            expectOk(Helpers.compile(gxFor(cache1), oneEntry));
        }

        writeModule(
                src.resolve("two.at"),
                TList.of(mb.call(mb.log(), importField(mb, "v", mb.sourceLocal(mb.str("one.at"))))));
        var twoEntry = new SourceSpecAbsolute.Local(src.resolve("two.at").toString());
        try (var cache2 = new CacheReal(cacheDir)) {
            var ok = expectOk(Helpers.compile(gxFor(cache2), twoEntry));
            assertEquals(1, ok.logs().size());
            assertEquals("cached-one", ok.logs().get(0).message());
        }
    }

    @Test
    public void test00360NewRootSeesChangedDependency(@TempDir Path tmp) throws IOException {
        var mb = new TestMb();
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var cacheDir = tmp.resolve("cache");
        var oneFile = src.resolve("one.at");

        writeModule(oneFile, TList.of(mb.return_(mb.str("v1"))));
        try (var cache = new CacheReal(cacheDir)) {
            expectOk(Helpers.compile(gxFor(cache), new SourceSpecAbsolute.Local(oneFile.toString())));
        }
        writeModule(oneFile, TList.of(mb.return_(mb.str("v2"))));
        Files.setLastModifiedTime(
                oneFile, FileTime.fromMillis(Files.getLastModifiedTime(oneFile).toMillis() + 100000));

        var twoFile = src.resolve("two.at");
        writeModule(twoFile, TList.of(mb.call(mb.log(), importField(mb, "v", mb.sourceLocal(mb.str("one.at"))))));

        try (var cache = new CacheReal(cacheDir)) {
            var ok = expectOk(Helpers.compile(gxFor(cache), new SourceSpecAbsolute.Local(twoFile.toString())));
            assertEquals("v2", ok.logs().get(0).message());
        }
    }

    @Test
    public void test00370DirectoryLockRejectsSecondInstance(@TempDir Path tmp) {
        var cacheDir = tmp.resolve("cache");
        try (var first = new CacheReal(cacheDir)) {
            var failed = false;
            try (var second = new CacheReal(cacheDir)) {
            } catch (RuntimeException e) {
                failed = true;
                var message = e.getMessage();
                assertTrue(
                        message != null && message.contains("already in use"), () -> "unexpected message: " + message);
            }
            assertTrue(failed, "second CacheReal on the same directory must be rejected");
        }
    }

    @Test
    public void test00380FailedCompileIsCachedThenRecompiledOnFix(@TempDir Path tmp) throws IOException {
        var mb = new TestMb();
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var cacheDir = tmp.resolve("cache");
        var mainFile = src.resolve("main.at");
        writeModule(mainFile, TList.of(mb.call(mb.log(), importField(mb, "v", mb.sourceLocal(mb.str("missing.at"))))));
        try (var cache = new CacheReal(cacheDir)) {
            var res = Helpers.compile(gxFor(cache), new SourceSpecAbsolute.Local(mainFile.toString()));
            assertTrue(
                    switch (res) {
                        case Result.Ok<?, ?> o -> false;
                        case Result.Err<?, ?> e -> true;
                    },
                    () -> "expected failure, got " + res);
        }
        try (var cache = new CacheReal(cacheDir)) {
            var res = Helpers.compile(gxFor(cache), new SourceSpecAbsolute.Local(mainFile.toString()));
            assertTrue(
                    switch (res) {
                        case Result.Ok<?, ?> o -> false;
                        case Result.Err<?, ?> e -> true;
                    },
                    () -> "expected cached failure, got " + res);
        }
        writeModule(mainFile, TList.of(mb.call(mb.log(), mb.str("fixed"))));
        Files.setLastModifiedTime(
                mainFile,
                FileTime.fromMillis(Files.getLastModifiedTime(mainFile).toMillis() + 100000));
        try (var cache = new CacheReal(cacheDir)) {
            var ok = expectOk(Helpers.compile(gxFor(cache), new SourceSpecAbsolute.Local(mainFile.toString())));
            assertEquals("fixed", ok.logs().get(0).message());
        }
    }

    @Test
    public void test00390SchemaMismatchRebuilds(@TempDir Path tmp) throws Exception {
        var mb = new TestMb();
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var cacheDir = tmp.resolve("cache");
        var mainFile = src.resolve("main.at");
        writeModule(mainFile, TList.of(mb.call(mb.log(), mb.str("hello"))));
        var entry = new SourceSpecAbsolute.Local(mainFile.toString());

        try (var cache = new CacheReal(cacheDir)) {
            expectOk(Helpers.compile(gxFor(cache), entry));
        }

        try (var conn = DriverManager.getConnection("jdbc:sqlite:" + cacheDir.resolve("cache.sqlite"));
                var st = conn.createStatement()) {
            st.executeUpdate("UPDATE schema_version SET version = 99");
        }

        try (var cache = new CacheReal(cacheDir)) {
            var ok = expectOk(Helpers.compile(gxFor(cache), entry));
            assertEquals("hello", ok.logs().get(0).message());
        }
    }

    @Test
    public void test00400RemoteAndBundle(@TempDir Path tmp) throws Exception {
        var mb = new TestMb();

        var singleBytes = moduleBytes(TList.of(mb.return_(mb.str("from-remote"))));

        var modBytes = moduleBytes(TList.of(mb.return_(mb.str("from-bundle-mod"))));
        var dataModBytes = moduleBytes(TList.of(mb.return_(mb.call(
                mb.field(
                        mb.field(
                                mb.field(
                                        mb.importRecord(
                                                TList.of(
                                                        new SourceExpr.RecordEntry(
                                                                mb.str("f"), mb.sourceLocal(mb.str("data/blob.bin"))))),
                                        mb.str("f")),
                                mb.str("len")),
                        mb.str("to_string")),
                mb.tupleOf()))));
        var zipOut = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(zipOut)) {
            zip.putNextEntry(new ZipEntry("mod.at"));
            zip.write(modBytes);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("data_mod.at"));
            zip.write(dataModBytes);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("data/blob.bin"));
            zip.write(new byte[] {9, 8, 7, 6, 5, 4, 3});
            zip.closeEntry();
        }
        var bundleBytes = zipOut.toByteArray();

        var server = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
        var thread = new Thread(() -> {
            while (!server.isClosed()) {
                try (var sock = server.accept()) {
                    var in = sock.getInputStream();
                    var requestLine = readLine(in);
                    while (!readLine(in).isEmpty()) {}
                    var path = requestLine.split(" ")[1];
                    var body = path.contains("bundle") ? bundleBytes : singleBytes;
                    var out = sock.getOutputStream();
                    out.write(("HTTP/1.1 200 OK\r\nContent-Length: " + body.length + "\r\nConnection: close\r\n\r\n")
                            .getBytes(StandardCharsets.US_ASCII));
                    out.write(body);
                    out.flush();
                } catch (IOException e) {
                    return;
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
        try {
            var base = "http://127.0.0.1:" + server.getLocalPort();
            var singleUrl = base + "/single.at";
            var bundleUrl = base + "/bundle.zip";

            var singleHash = hashSpec(mb, sha256(singleBytes));
            var bundleHash = hashSpec(mb, sha256(bundleBytes));

            var remoteSingle = importField(mb, "v", mb.sourceRemote(mb.str(singleUrl), singleHash));
            var bundleRecord = mb.importRecord(
                    TList.of(new SourceExpr.RecordEntry(mb.str("b"), mb.sourceRemote(mb.str(bundleUrl), bundleHash))));
            var bundleMod = mb.field(mb.field(bundleRecord, mb.str("b")), mb.str("mod"));
            var bundleDataMod = mb.field(mb.field(bundleRecord, mb.str("b")), mb.str("data_mod"));

            var mainExpr = mb.seqOf(
                    mb.call(mb.log(), remoteSingle), mb.call(mb.log(), bundleMod), mb.call(mb.log(), bundleDataMod));
            var src = tmp.resolve("src");
            Files.createDirectories(src);
            var mainFile = src.resolve("main.at");
            writeModule(mainFile, TList.of(mainExpr));

            try (var cache = new CacheReal(tmp.resolve("cache"))) {
                var ok = expectOk(Helpers.compile(gxFor(cache), new SourceSpecAbsolute.Local(mainFile.toString())));
                assertEquals(3, ok.logs().size());
                assertEquals("from-remote", ok.logs().get(0).message());
                assertEquals("from-bundle-mod", ok.logs().get(1).message());
                assertEquals("7", ok.logs().get(2).message());
            }
            var wrongDigest = sha256(singleBytes);
            wrongDigest[0] ^= 0x01;
            var badFile = src.resolve("bad.at");
            writeModule(
                    badFile,
                    TList.of(mb.call(
                            mb.log(),
                            importField(mb, "v", mb.sourceRemote(mb.str(singleUrl), hashSpec(mb, wrongDigest))))));
            try (var cache = new CacheReal(tmp.resolve("cache"))) {
                var res = Helpers.compile(gxFor(cache), new SourceSpecAbsolute.Local(badFile.toString()));
                assertTrue(
                        switch (res) {
                            case Result.Ok<?, ?> o -> false;
                            case Result.Err<?, ?> e -> true;
                        },
                        () -> "expected hash mismatch, got " + res);
            }
        } finally {
            server.close();
        }
    }

    private static String readLine(InputStream in) throws IOException {
        var sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\r') continue;
            if (c == '\n') break;
            sb.append((char) c);
        }
        return sb.toString();
    }

    private static String git(Path dir, String... args) throws Exception {
        var argv = new ArrayList<String>();
        argv.add("git");
        argv.addAll(List.of(args));
        var proc = new ProcessBuilder(argv)
                .directory(dir.toFile())
                .redirectErrorStream(true)
                .start();
        var out = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (proc.waitFor() != 0) throw new AssertionError("git " + String.join(" ", args) + " failed: " + out);
        return out.trim();
    }

    @Test
    public void test00500GitOrigin(@TempDir Path tmp) throws Exception {
        var gitOnPath = false;
        var path = System.getenv("PATH");
        if (path != null) {
            for (var dir : path.split(File.pathSeparator)) gitOnPath |= Files.isExecutable(Path.of(dir, "git"));
        }
        Assumptions.assumeTrue(gitOnPath, "git not on PATH");
        var mb = new TestMb();
        var repo = tmp.resolve("repo");
        Files.createDirectories(repo.resolve("lib"));
        Files.write(repo.resolve("lib/mod.at"), moduleBytes(TList.of(mb.return_(mb.str("from-git-module")))));
        Files.write(repo.resolve("lib/blob.bin"), new byte[] {9, 8, 7, 6});
        git(repo, "init", "-q", "-b", "main", ".");
        git(repo, "-c", "user.name=t", "-c", "user.email=t@t", "add", ".");
        git(repo, "-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "-m", "spec");
        var commit = git(repo, "rev-parse", "HEAD");
        var url = repo.toUri().toString();

        var b = mb.field(
                mb.importRecord(
                        TList.of(new SourceExpr.RecordEntry(mb.str("b"), mb.sourceGit(mb.str(url), mb.str(commit))))),
                mb.str("b"));
        var mainExpr = mb.seqOf(
                mb.call(mb.log(), mb.field(mb.field(b, mb.str("lib")), mb.str("mod"))),
                mb.call(
                        mb.log(),
                        mb.call(
                                mb.field(
                                        mb.field(
                                                mb.field(mb.field(b, mb.str("lib")), mb.str("blob.bin")),
                                                mb.str("len")),
                                        mb.str("to_string")),
                                mb.tupleOf())));
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var mainFile = src.resolve("main.at");
        writeModule(mainFile, TList.of(mainExpr));
        try (var cache = new CacheReal(tmp.resolve("cache"))) {
            var ok = expectOk(Helpers.compile(gxFor(cache), new SourceSpecAbsolute.Local(mainFile.toString())));
            assertEquals("from-git-module", ok.logs().get(0).message());
            assertEquals("4", ok.logs().get(1).message());
        }
        var bad = mb.importRecord(
                TList.of(new SourceExpr.RecordEntry(mb.str("b"), mb.sourceGit(mb.str(url), mb.str("main")))));
        writeModule(mainFile, TList.of(mb.call(mb.log(), mb.field(bad, mb.str("b")))));
        try (var cache = new CacheReal(tmp.resolve("cache2"))) {
            var res = Helpers.compile(gxFor(cache), new SourceSpecAbsolute.Local(mainFile.toString()));
            assertTrue(
                    switch (res) {
                        case Result.Err<?, ?> err -> err.error().toString().contains("40-digit");
                        case Result.Ok<?, ?> ok -> false;
                    },
                    () -> "" + res);
        }
    }

    private static SourceExpr hashSpec(TestMb mb, byte[] digest) {
        var decoded = mb.call(mb.builtin("zbase32", "decode"), mb.str(Zbase32.encode(digest)));
        return mb.call(mb.builtin("source", "sha256"), decoded);
    }
}
