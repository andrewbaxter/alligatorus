package compilerxyz.tests;


import com.google.gson.stream.JsonWriter;
import compilerxyz.eval.SourceId;
import compilerxyz.eval.builtin.Zbase32;
import compilerxyz.gen.codec.SourceModuleCodec;
import compilerxyz.langhelp.Compile;
import compilerxyz.langhelp.GlobalContext;
import compilerxyz.langhelp.Sources;
import compilerxyz.langhelp.cache.CacheReal;
import compilerxyz.safejson.Json;
import compilerxyz.syntax.SourceExpr;
import compilerxyz.syntax.SourceModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import java.io.ByteArrayOutputStream;
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
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class T0180ImportRealTest {

  private static String serializeModule(SourceExpr expr) throws IOException {
    var sw = new StringWriter();
    var writer = new JsonWriter(sw);
    writer.setSerializeNulls(true);
    writer.setIndent("");
    SourceModuleCodec.encode(writer, new SourceModule.V1(expr), Json.Style.COMPACT);
    writer.close();
    return sw.toString();
  }

  private static void writeModule(Path file, SourceExpr expr) throws IOException {
    Files.writeString(file, serializeModule(expr));
  }

  private static byte[] moduleBytes(SourceExpr expr) throws IOException {
    return serializeModule(expr).getBytes(StandardCharsets.UTF_8);
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
    writeModule(src.resolve("other.at"), mb.str("from-other"));
    writeModule(
        src.resolve("main.at"),
        mb.call(mb.log(), importField(mb, "v", mb.sourceLocal(mb.str("other.at")))));

    try (var cache = new CacheReal(tmp.resolve("cache"))) {
      var res =
          Compile.compile(
              gxFor(cache), new SourceSpecAbsolute.Local(src.resolve("main.at").toString()));
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
    writeModule(src.resolve("a.at"), mb.str("AA"));
    writeModule(src.resolve("b.at"), mb.str("BB"));
    var both =
        new SourceExpr.RecordEntry(mb.str("a"), mb.sourceLocal(mb.str("a.at")));
    var second =
        new SourceExpr.RecordEntry(mb.str("b"), mb.sourceLocal(mb.str("b.at")));
    var rec = mb.importRecord(TList.of(both, second));
    writeModule(
        src.resolve("main.at"),
        mb.seqOf(
            mb.call(mb.log(), mb.field(rec, mb.str("a"))),
            mb.call(mb.log(), mb.field(rec, mb.str("b")))));

    try (var cache = new CacheReal(tmp.resolve("cache"))) {
      var ok =
          expectOk(
              Compile.compile(
                  gxFor(cache), new SourceSpecAbsolute.Local(src.resolve("main.at").toString())));
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
    writeModule(otherFile, mb.str("v1"));
    writeModule(
        mainFile, mb.call(mb.log(), importField(mb, "v", mb.sourceLocal(mb.str("other.at")))));
    var entry = new SourceSpecAbsolute.Local(mainFile.toString());

    try (var cache = new CacheReal(cacheDir)) {
      var ok = expectOk(Compile.compile(gxFor(cache), entry));
      assertEquals("v1", ok.logs().get(0).message());
    }

    writeModule(otherFile, mb.str("v2"));
    Files.setLastModifiedTime(
        otherFile, FileTime.fromMillis(Files.getLastModifiedTime(otherFile).toMillis() + 100000));

    try (var cache = new CacheReal(cacheDir)) {
      var ok = expectOk(Compile.compile(gxFor(cache), entry));
      assertEquals("v2", ok.logs().get(0).message());
    }
  }

  @Test
  public void test00350PersistentCacheAcrossContexts(@TempDir Path tmp) throws IOException {
      var mb = new TestMb();
      var src = tmp.resolve("src");
      Files.createDirectories(src);
      var cacheDir = tmp.resolve("cache");

      writeModule(src.resolve("one.at"), mb.str("cached-one"));
      var oneEntry = new SourceSpecAbsolute.Local(src.resolve("one.at").toString());
      try (var cache1 = new CacheReal(cacheDir)) {
          expectOk(Compile.compile(gxFor(cache1), oneEntry));
      }

      writeModule(
              src.resolve("two.at"),
              mb.call(mb.log(), importField(mb, "v", mb.sourceLocal(mb.str("one.at")))));
      var twoEntry = new SourceSpecAbsolute.Local(src.resolve("two.at").toString());
      try (var cache2 = new CacheReal(cacheDir)) {
          var ok = expectOk(Compile.compile(gxFor(cache2), twoEntry));
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

      writeModule(oneFile, mb.str("v1"));
      try (var cache = new CacheReal(cacheDir)) {
          expectOk(Compile.compile(gxFor(cache), new SourceSpecAbsolute.Local(oneFile.toString())));
      }
      writeModule(oneFile, mb.str("v2"));
      Files.setLastModifiedTime(
              oneFile, FileTime.fromMillis(Files.getLastModifiedTime(oneFile).toMillis() + 100000));

      var twoFile = src.resolve("two.at");
      writeModule(
              twoFile, mb.call(mb.log(), importField(mb, "v", mb.sourceLocal(mb.str("one.at")))));

      try (var cache = new CacheReal(cacheDir)) {
          var ok =
                  expectOk(Compile.compile(gxFor(cache), new SourceSpecAbsolute.Local(twoFile.toString())));
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
        assertTrue(
            e.getMessage().contains("already in use"), () -> "unexpected message: " + e.getMessage());
      }
      assertTrue(failed, "second CacheReal on the same directory must be rejected");
    }
  }

  @Test
  public void test00380FailedCompileIsCachedThenRecompiledOnFix(@TempDir Path tmp)
      throws IOException {
      var mb = new TestMb();
      var src = tmp.resolve("src");
      Files.createDirectories(src);
      var cacheDir = tmp.resolve("cache");
      var mainFile = src.resolve("main.at");
      writeModule(
              mainFile, mb.call(mb.log(), importField(mb, "v", mb.sourceLocal(mb.str("missing.at")))));
      try (var cache = new CacheReal(cacheDir)) {
          var res = Compile.compile(gxFor(cache), new SourceSpecAbsolute.Local(mainFile.toString()));
          assertTrue(
                  switch (res) {
                      case Result.Ok<?, ?> o -> false;
                      case Result.Err<?, ?> e -> true;
                  },
                  () -> "expected failure, got " + res);
      }
      try (var cache = new CacheReal(cacheDir)) {
          var res = Compile.compile(gxFor(cache), new SourceSpecAbsolute.Local(mainFile.toString()));
          assertTrue(
                  switch (res) {
                      case Result.Ok<?, ?> o -> false;
                      case Result.Err<?, ?> e -> true;
                  },
                  () -> "expected cached failure, got " + res);
      }
      writeModule(mainFile, mb.call(mb.log(), mb.str("fixed")));
      Files.setLastModifiedTime(
              mainFile, FileTime.fromMillis(Files.getLastModifiedTime(mainFile).toMillis() + 100000));
      try (var cache = new CacheReal(cacheDir)) {
          var ok =
                  expectOk(Compile.compile(gxFor(cache), new SourceSpecAbsolute.Local(mainFile.toString())));
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
    writeModule(mainFile, mb.call(mb.log(), mb.str("hello")));
    var entry = new SourceSpecAbsolute.Local(mainFile.toString());

    try (var cache = new CacheReal(cacheDir)) {
      expectOk(Compile.compile(gxFor(cache), entry));
    }

    try (var conn =
            DriverManager.getConnection(
                "jdbc:sqlite:" + cacheDir.resolve("cache.sqlite"));
        var st = conn.createStatement()) {
      st.executeUpdate("UPDATE schema_version SET version = 99");
    }

    try (var cache = new CacheReal(cacheDir)) {
      var ok = expectOk(Compile.compile(gxFor(cache), entry));
      assertEquals("hello", ok.logs().get(0).message());
    }
  }

  @Test
  public void test00400RemoteAndBundle(@TempDir Path tmp) throws Exception {
    var mb = new TestMb();

    var singleBytes = moduleBytes(mb.str("from-remote"));

    var modBytes = moduleBytes(mb.str("from-bundle-mod"));
    var zipOut = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(zipOut)) {
      zip.putNextEntry(new ZipEntry("mod.at"));
      zip.write(modBytes);
      zip.closeEntry();
    }
    var bundleBytes = zipOut.toByteArray();

    var server = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
    var thread =
        new Thread(
            () -> {
              while (!server.isClosed()) {
                try (var sock = server.accept()) {
                  var in = sock.getInputStream();
                  var requestLine = readLine(in);
                  while (!readLine(in).isEmpty()) {}
                  var path = requestLine.split(" ")[1];
                  var body = path.contains("bundle") ? bundleBytes : singleBytes;
                  var out = sock.getOutputStream();
                  out.write(
                      ("HTTP/1.1 200 OK\r\nContent-Length: "
                              + body.length
                              + "\r\nConnection: close\r\n\r\n")
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
        var bundleRecord =
                mb.importRecord(
                        TList.of(
                                new SourceExpr.RecordEntry(
                                        mb.str("b"), mb.sourceRemote(mb.str(bundleUrl), bundleHash))));
        var bundleMod = mb.field(mb.field(bundleRecord, mb.str("b")), mb.str("mod"));

        var mainExpr = mb.seqOf(mb.call(mb.log(), remoteSingle), mb.call(mb.log(), bundleMod));
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var mainFile = src.resolve("main.at");
        writeModule(mainFile, mainExpr);

        try (var cache = new CacheReal(tmp.resolve("cache"))) {
            var ok =
                    expectOk(
                            Compile.compile(
                                    gxFor(cache), new SourceSpecAbsolute.Local(mainFile.toString())));
            assertEquals(2, ok.logs().size());
            assertEquals("from-remote", ok.logs().get(0).message());
            assertEquals("from-bundle-mod", ok.logs().get(1).message());
        }
        var wrongDigest = sha256(singleBytes);
        wrongDigest[0] ^= 0x01;
        var badFile = src.resolve("bad.at");
        writeModule(
                badFile,
                mb.call(
                        mb.log(),
                        importField(
                                mb, "v", mb.sourceRemote(mb.str(singleUrl), hashSpec(mb, wrongDigest)))));
        try (var cache = new CacheReal(tmp.resolve("cache"))) {
            var res =
                    Compile.compile(gxFor(cache), new SourceSpecAbsolute.Local(badFile.toString()));
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

  private static SourceExpr hashSpec(TestMb mb, byte[] digest) {
    var decoded = mb.call(mb.builtin("zbase32", "decode"), mb.str(Zbase32.encode(digest)));
    return mb.call(mb.builtin("source", "sha256"), decoded);
  }
}
