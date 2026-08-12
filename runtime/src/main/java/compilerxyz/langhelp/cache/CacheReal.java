package compilerxyz.langhelp.cache;

import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ArtifactJson;
import compilerxyz.artifact.Artifacts;
import compilerxyz.eval.CompileId;
import compilerxyz.eval.CompileRes;
import compilerxyz.eval.CompileResResult;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.ExprSubErr;
import compilerxyz.eval.ImportStateId;
import compilerxyz.eval.LogEntry;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.SourceId;
import compilerxyz.langhelp.Compiles;
import compilerxyz.langhelp.Sources;
import compilerxyz.safejson.Json;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueArray;
import compilerxyz.safejson.JsonValueNumber;
import compilerxyz.safejson.JsonValueObject;
import compilerxyz.safejson.JsonValueString;
import compilerxyz.syntax.HashValue;
import compilerxyz.syntax.SourceModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import compilerxyz.util.TOrderedMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.zip.ZipInputStream;
import com.google.gson.stream.JsonWriter;
import java.io.StringWriter;
import java.nio.channels.OverlappingFileLockException;
import java.util.zip.ZipEntry;

public final class CacheReal implements Sources, Artifacts, Compiles, AutoCloseable {
  private static final int SCHEMA_VERSION = 2;
  private static final QExprId DUMMY_LOC = new QExprId(new SourceId(0), 0);

  private final Connection conn;
  private final Object dbLock = new Object();
  private final FileChannel lockChannel;
  private final FileLock dirLock;
  private final HttpClient http =
      HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

  private final TMap<ArtifactIdSource, ArtifactBody> artifactMem = new TMap<>();
  private final TMap<ArtifactIdSource, ArtifactBody> pendingArtifacts = new TMap<>();
  private final TMap<CompileId, CompileSlot> compileMem = new TMap<>();
  private final ReentrantLock compileMapLock = new ReentrantLock();

  private static final class CompileSlot {
    final ReentrantLock lock = new ReentrantLock();
    CompileRes value;
  }

  public CacheReal() {
    this(cacheDir());
  }

  public CacheReal(Path dir) {
    try {
      Class.forName("org.sqlite.JDBC");
      Files.createDirectories(dir);
      lockChannel =
          FileChannel.open(
              dir.resolve("lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
      FileLock acquired;
      try {
        acquired = lockChannel.tryLock();
      } catch (OverlappingFileLockException e) {
        acquired = null;
      }
      if (acquired == null) {
        lockChannel.close();
        throw new RuntimeException(
            "CacheReal: cache directory is already in use by another instance: " + dir);
      }
      dirLock = acquired;
      conn = DriverManager.getConnection("jdbc:sqlite:" + dir.resolve("cache.sqlite"));
      conn.setAutoCommit(true);
      initSchema();
    } catch (ClassNotFoundException | SQLException | IOException e) {
      throw new RuntimeException("CacheReal: failed to open cache database", e);
    }
  }

  public static Path cacheDir() {
    var env = System.getenv("ALLIG_CACHE_DIR");
    Path base;
    if (env != null && !env.isBlank()) {
      base = Path.of(env);
    } else {
      var os = System.getProperty("os.name", "").toLowerCase();
      var home = System.getProperty("user.home");
      if (os.contains("win")) {
        var local = System.getenv("LOCALAPPDATA");
        base = local != null && !local.isBlank() ? Path.of(local) : Path.of(home, "AppData", "Local");
      } else if (os.contains("mac")) {
        base = Path.of(home, "Library", "Caches");
      } else {
        var xdg = System.getenv("XDG_CACHE_HOME");
        base = xdg != null && !xdg.isBlank() ? Path.of(xdg) : Path.of(home, ".cache");
      }
    }
    return base.resolve("allig");
  }

  private void initSchema() throws SQLException {
    synchronized (dbLock) {
      var existing = readSchemaVersion();
      if (existing.optIsSome() && existing.optUnwrap() != SCHEMA_VERSION) {
        System.err.println(
            "warning: allig cache schema version "
                + existing.optUnwrap()
                + " does not match expected "
                + SCHEMA_VERSION
                + "; rebuilding cache");
        dropAll();
      }
      createAll();
      if (readSchemaVersion().optIsNone()) {
        try (var ins = conn.prepareStatement("INSERT INTO schema_version (version) VALUES (?)")) {
          ins.setInt(1, SCHEMA_VERSION);
          ins.executeUpdate();
        }
      }
      try (var ins = conn.prepareStatement("INSERT OR IGNORE INTO artifact_seq (id, next) VALUES (0, 0)")) {
        ins.executeUpdate();
      }
      try (var ins = conn.prepareStatement("INSERT OR IGNORE INTO source_seq (id, next) VALUES (0, 0)")) {
        ins.executeUpdate();
      }
    }
  }

  private Opt<Integer> readSchemaVersion() throws SQLException {
    try (var st = conn.createStatement()) {
      st.executeUpdate("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER NOT NULL)");
    }
    try (var rs = conn.createStatement().executeQuery("SELECT version FROM schema_version")) {
      return rs.next() ? Opt.some(rs.getInt(1)) : Opt.none();
    }
  }

  private void dropAll() throws SQLException {
    try (var st = conn.createStatement()) {
      st.executeUpdate("DROP TABLE IF EXISTS schema_version");
      st.executeUpdate("DROP TABLE IF EXISTS artifact_seq");
      st.executeUpdate("DROP TABLE IF EXISTS source_seq");
      st.executeUpdate("DROP TABLE IF EXISTS sources");
      st.executeUpdate("DROP TABLE IF EXISTS remote_bytes");
      st.executeUpdate("DROP TABLE IF EXISTS artifacts");
      st.executeUpdate("DROP TABLE IF EXISTS compiles");
    }
  }

  private void createAll() throws SQLException {
    try (var st = conn.createStatement()) {
      st.executeUpdate("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER NOT NULL)");
      st.executeUpdate(
          "CREATE TABLE IF NOT EXISTS artifact_seq (id INTEGER PRIMARY KEY CHECK (id = 0), next INTEGER NOT NULL)");
      st.executeUpdate(
          "CREATE TABLE IF NOT EXISTS source_seq (id INTEGER PRIMARY KEY CHECK (id = 0), next INTEGER NOT NULL)");
      st.executeUpdate(
          "CREATE TABLE IF NOT EXISTS sources (spec_key TEXT PRIMARY KEY, source_id INTEGER NOT NULL)");
      st.executeUpdate(
          "CREATE TABLE IF NOT EXISTS remote_bytes (url TEXT PRIMARY KEY, bytes BLOB NOT NULL)");
      st.executeUpdate(
          "CREATE TABLE IF NOT EXISTS artifacts ("
              + "import_state INTEGER NOT NULL, source INTEGER NOT NULL, idx INTEGER NOT NULL,"
              + " body TEXT NOT NULL, PRIMARY KEY (import_state, source, idx))");
      st.executeUpdate(
          "CREATE TABLE IF NOT EXISTS compiles ("
              + "import_state INTEGER NOT NULL, source INTEGER NOT NULL, result TEXT NOT NULL,"
              + " local_path TEXT, local_mtime INTEGER, local_deps TEXT,"
              + " PRIMARY KEY (import_state, source))");
    }
  }

  /** Allocates the next value of a persistent counter, read and advanced in the database. */
  private long nextSeq(String table) {
    synchronized (dbLock) {
      try {
        long current;
        try (var ps = conn.prepareStatement("SELECT next FROM " + table + " WHERE id = 0");
            var rs = ps.executeQuery()) {
          current = rs.next() ? rs.getLong(1) : 0L;
        }
        try (var ps = conn.prepareStatement("UPDATE " + table + " SET next = ? WHERE id = 0")) {
          ps.setLong(1, current + 1);
          ps.executeUpdate();
        }
        return current;
      } catch (SQLException e) {
        throw new RuntimeException("CacheReal.nextSeq " + table, e);
      }
    }
  }

  private SourceId sourceIdFor(String specKey) {
    synchronized (dbLock) {
      try {
        try (var ps = conn.prepareStatement("SELECT source_id FROM sources WHERE spec_key = ?")) {
          ps.setString(1, specKey);
          try (var rs = ps.executeQuery()) {
            if (rs.next()) return new SourceId(rs.getLong(1));
          }
        }
        var id = nextSeq("source_seq");
        try (var ps = conn.prepareStatement("INSERT INTO sources (spec_key, source_id) VALUES (?, ?)")) {
          ps.setString(1, specKey);
          ps.setLong(2, id);
          ps.executeUpdate();
        }
        return new SourceId(id);
      } catch (SQLException e) {
        throw new RuntimeException("CacheReal.sourceIdFor", e);
      }
    }
  }

  @Override
  public Result<SourceResolved, String> sourcesGet(SourceSpecAbsolute spec) {
    return switch (spec) {
      case SourceSpecAbsolute.Virtual v ->
          Result.err("virtual sources are not supported by the real cache: " + v.name());
      case SourceSpecAbsolute.Local l -> resolveLocal(l);
      case SourceSpecAbsolute.Remote r -> resolveRemote(r);
    };
  }

  private Result<SourceResolved, String> resolveLocal(SourceSpecAbsolute.Local l) {
    var file = Path.of(l.path());
    if (!Files.isRegularFile(file)) {
      return Result.err("local source not found: " + l.path());
    }
    String text;
    long mtime;
    try {
      text = Files.readString(file);
      mtime = Files.getLastModifiedTime(file).toMillis();
    } catch (IOException e) {
      return Result.err("failed to read local source " + l.path() + ": " + e.getMessage());
    }
    SourceModule module;
    try {
      module = Json.deserialize(SourceModule.class, text);
    } catch (IOException | RuntimeException e) {
      return Result.err("failed to parse local source " + l.path() + ": " + e.getMessage());
    }
    var id = sourceIdFor(l.sourceSpecAbsoluteSpecKey());
    return Result.ok(
        new SourceResolved.Module(id, module, Opt.some(new Compiles.LocalMeta(l.path(), mtime))));
  }

  private Result<SourceResolved, String> resolveRemote(SourceSpecAbsolute.Remote r) {
    var bytesRes = fetchRemote(r.baseUrl(), r.hash());
    byte[] bytes;
    switch (bytesRes) {
      case Result.Err<byte[], String> err -> { return Result.err(err.error()); }
      case Result.Ok<byte[], String> ok -> bytes = ok.value();
    }
    var isZip = looksLikeZip(bytes);
    if (r.subpath().isEmpty()) {
      if (isZip) return Result.ok(new SourceResolved.Bundle(r));
      return parseRemoteModule(r, bytes);
    }
    if (!isZip) {
      return Result.err("remote source is not a bundle but a subpath was requested: " + r.subpath());
    }
    var entryBytes = extractZipEntry(bytes, r.subpath());
    if (entryBytes == null) {
      return Result.err("bundle entry not found: " + r.subpath());
    }
    return parseRemoteModule(r, entryBytes);
  }

  private Result<SourceResolved, String> parseRemoteModule(SourceSpecAbsolute.Remote r, byte[] bytes) {
    SourceModule module;
    try {
      module = Json.deserialize(SourceModule.class, new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
    } catch (IOException | RuntimeException e) {
      return Result.err("failed to parse remote source " + r.baseUrl() + ": " + e.getMessage());
    }
    var id = sourceIdFor(r.sourceSpecAbsoluteSpecKey());
    return Result.ok(new SourceResolved.Module(id, module, Opt.none()));
  }

  private Result<byte[], String> fetchRemote(String url, HashValue hash) {
    var cached = loadRemoteBytes(url);
    if (cached != null) {
      return verifyHash(url, cached, hash);
    }
    byte[] bytes;
    try {
      var response =
          http.send(
              HttpRequest.newBuilder(URI.create(url)).GET().build(),
              HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() != 200) {
        return Result.err("remote fetch failed (" + response.statusCode() + "): " + url);
      }
      bytes = response.body();
    } catch (IOException e) {
      return Result.err("remote fetch failed: " + url + ": " + e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Result.err("remote fetch interrupted: " + url);
    }
    var verified = verifyHash(url, bytes, hash);
    switch (verified) {
      case Result.Err<byte[], String> ignored -> { return verified; }
      case Result.Ok<byte[], String> ignored -> {}
    }
    storeRemoteBytes(url, bytes);
    return verified;
  }

  private static Result<byte[], String> verifyHash(String url, byte[] bytes, HashValue hash) {
    var actual = sha256(bytes);
    switch (hash) {
      case HashValue.Sha256 expected -> {
        if (!java.util.Arrays.equals(actual, expected.digest())) {
          return Result.err(
              "remote hash mismatch for " + url + ": expected " + hash.hashValueHashKey()
                  + ", got sha256:" + java.util.HexFormat.of().formatHex(actual));
        }
      }
    }
    return Result.ok(bytes);
  }

  private static byte[] sha256(byte[] data) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(data);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 unavailable", e);
    }
  }

  private static boolean looksLikeZip(byte[] bytes) {
    return bytes.length >= 4
        && bytes[0] == 'P'
        && bytes[1] == 'K'
        && bytes[2] == 3
        && bytes[3] == 4;
  }

  private static byte[] extractZipEntry(byte[] zipBytes, String entryName) {
    try (var zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
      ZipEntry entry;
      while ((entry = zin.getNextEntry()) != null) {
        if (entry.getName().equals(entryName)) {
          return zin.readAllBytes();
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("CacheReal.extractZipEntry", e);
    }
    return null;
  }

  private byte[] loadRemoteBytes(String url) {
    synchronized (dbLock) {
      try (var ps = conn.prepareStatement("SELECT bytes FROM remote_bytes WHERE url = ?")) {
        ps.setString(1, url);
        try (var rs = ps.executeQuery()) {
          return rs.next() ? rs.getBytes(1) : null;
        }
      } catch (SQLException e) {
        throw new RuntimeException("CacheReal.loadRemoteBytes", e);
      }
    }
  }

  private void storeRemoteBytes(String url, byte[] bytes) {
    synchronized (dbLock) {
      try (var ps = conn.prepareStatement("INSERT OR REPLACE INTO remote_bytes (url, bytes) VALUES (?, ?)")) {
        ps.setString(1, url);
        ps.setBytes(2, bytes);
        ps.executeUpdate();
      } catch (SQLException e) {
        throw new RuntimeException("CacheReal.storeRemoteBytes", e);
      }
    }
  }

  @Override
  public synchronized Result<ArtifactBody, String> artifactsGet(ArtifactIdSource id) {
    var mem = artifactMem.getOrNull(id);
    if (mem != null) return Result.ok(mem);
    synchronized (dbLock) {
      try (var ps =
          conn.prepareStatement(
              "SELECT body FROM artifacts WHERE import_state = ? AND source = ? AND idx = ?")) {
        ps.setLong(1, id.compileId().importState().value());
        ps.setLong(2, id.compileId().source().value());
        ps.setLong(3, id.index());
        try (var rs = ps.executeQuery()) {
          if (!rs.next()) return Result.err("Unknown artifact: " + id);
          var body = decodeArtifactBody(rs.getString(1));
          artifactMem.put(id, body);
          return Result.ok(body);
        }
      } catch (SQLException e) {
        throw new RuntimeException("CacheReal.artifactsGet", e);
      }
    }
  }

  @Override
  public ArtifactIdSource artifactsReserveId() {
    var index = nextSeq("artifact_seq");
    return new ArtifactIdSource(new CompileId(new ImportStateId(0), new SourceId(0)), index);
  }

  @Override
  public synchronized void artifactsSet(ArtifactIdSource id, ArtifactBody body) {
    artifactMem.put(id, body);
    pendingArtifacts.put(id, body);
  }

  private synchronized TMap<ArtifactIdSource, ArtifactBody> takePendingArtifacts() {
    var out = new TMap<ArtifactIdSource, ArtifactBody>();
    for (var e : pendingArtifacts.entrySet()) out.put(e.getKey(), e.getValue());
    pendingArtifacts.clear();
    return out;
  }

  @Override
  public void compilesPrepare(CompileId root) {
    evictIfStale(root);
  }

  /**
   * Recursively validates a cached compile: an entry is stale if its own source file changed or
   * any recorded local dependency is stale. Stale entries are evicted from the database and the
   * in-memory slot map. The root and every dependency go through this same path.
   */
  private boolean evictIfStale(CompileId id) {
    synchronized (dbLock) {
      return staleWalk(id, new HashMap<>(), new HashSet<>());
    }
  }

  private boolean staleWalk(CompileId id, Map<CompileId, Boolean> memo, Set<CompileId> visiting) {
    var cached = memo.get(id);
    if (cached != null) return cached;
    if (!visiting.add(id)) return false;
    var row = loadCompileRow(id);
    if (row == null) {
      memo.put(id, false);
      visiting.remove(id);
      return false;
    }
    var stale = false;
    if (row.localPath != null) {
      var current = currentMtime(row.localPath);
      if (current == null || current != row.localMtime) stale = true;
    }
    for (var dep : row.localDeps) {
      if (staleWalk(dep, memo, visiting)) stale = true;
    }
    if (stale) {
      deleteCompile(id);
      compileMapLock.lock();
      try {
        compileMem.remove(id);
      } finally {
        compileMapLock.unlock();
      }
    }
    memo.put(id, stale);
    visiting.remove(id);
    return stale;
  }

  private static Long currentMtime(String path) {
    var file = Path.of(path);
    if (!Files.isRegularFile(file)) return null;
    try {
      return Files.getLastModifiedTime(file).toMillis();
    } catch (IOException e) {
      return null;
    }
  }

  private record CompileRow(String localPath, long localMtime, TList<CompileId> localDeps) {}

  private CompileRow loadCompileRow(CompileId id) {
    try (var ps =
        conn.prepareStatement(
            "SELECT local_path, local_mtime, local_deps FROM compiles WHERE import_state = ? AND source = ?")) {
      ps.setLong(1, id.importState().value());
      ps.setLong(2, id.source().value());
      try (var rs = ps.executeQuery()) {
        if (!rs.next()) return null;
        var localPath = rs.getString(1);
        var localMtime = rs.getLong(2);
        var deps = decodeCompileIds(rs.getString(3));
        return new CompileRow(localPath, localMtime, deps);
      }
    } catch (SQLException e) {
      throw new RuntimeException("CacheReal.loadCompileRow", e);
    }
  }

  private void deleteCompile(CompileId id) {
    try (var ps = conn.prepareStatement("DELETE FROM compiles WHERE import_state = ? AND source = ?")) {
      ps.setLong(1, id.importState().value());
      ps.setLong(2, id.source().value());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("CacheReal.deleteCompile", e);
    }
  }

  @Override
  public CompileRes compilesGetOrCompute(CompileId id, Supplier<CompileProduct> compute) {
    CompileSlot slot;
    compileMapLock.lock();
    try {
      slot = compileMem.getOrNull(id);
      if (slot == null) {
        slot = new CompileSlot();
        compileMem.put(id, slot);
      }
    } finally {
      compileMapLock.unlock();
    }
    slot.lock.lock();
    try {
      if (slot.value != null) return slot.value;
      evictIfStale(id);
      var fromDb = loadCompile(id);
      if (fromDb != null) {
        slot.value = fromDb;
        return fromDb;
      }
      var product = compute.get();
      slot.value = product.res();
      flush(id, product);
      return slot.value;
    } finally {
      slot.lock.unlock();
    }
  }

  /**
   * Commits the batched artifact writes accumulated during this compile together with the
   * compile result row in a single transaction.
   */
  private void flush(CompileId id, CompileProduct product) {
    var artifacts = takePendingArtifacts();
    synchronized (dbLock) {
      try {
        conn.setAutoCommit(false);
        try {
          try (var ps =
              conn.prepareStatement(
                  "INSERT OR REPLACE INTO artifacts (import_state, source, idx, body) VALUES (?, ?, ?, ?)")) {
            for (var e : artifacts.entrySet()) {
              var aid = e.getKey();
              ps.setLong(1, aid.compileId().importState().value());
              ps.setLong(2, aid.compileId().source().value());
              ps.setLong(3, aid.index());
              ps.setString(4, encodeArtifactBody(e.getValue()));
              ps.addBatch();
            }
            ps.executeBatch();
          }
          try (var ps =
              conn.prepareStatement(
                  "INSERT OR REPLACE INTO compiles"
                      + " (import_state, source, result, local_path, local_mtime, local_deps)"
                      + " VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setLong(1, id.importState().value());
            ps.setLong(2, id.source().value());
            ps.setString(3, encodeCompileRes(product.res()));
            if (product.local().optIsSome()) {
              var localMeta = product.local().optUnwrap();
              ps.setString(4, localMeta.path());
              ps.setLong(5, localMeta.timestampMs());
            } else {
              ps.setNull(4, java.sql.Types.VARCHAR);
              ps.setNull(5, java.sql.Types.INTEGER);
            }
            ps.setString(6, encodeCompileIds(product.localDeps()));
            ps.executeUpdate();
          }
          conn.commit();
        } catch (SQLException e) {
          conn.rollback();
          throw e;
        } finally {
          conn.setAutoCommit(true);
        }
      } catch (SQLException e) {
        throw new RuntimeException("CacheReal.flush", e);
      }
    }
  }

  @Override
  public TList<CompileRes> compilesResults() {
    compileMapLock.lock();
    try {
      var out = new TList<CompileRes>();
      for (var slot : compileMem.values()) {
        if (slot.value != null) out.add(slot.value);
      }
      return out;
    } finally {
      compileMapLock.unlock();
    }
  }

  private CompileRes loadCompile(CompileId id) {
    synchronized (dbLock) {
      try (var ps =
          conn.prepareStatement("SELECT result FROM compiles WHERE import_state = ? AND source = ?")) {
        ps.setLong(1, id.importState().value());
        ps.setLong(2, id.source().value());
        try (var rs = ps.executeQuery()) {
          if (!rs.next()) return null;
          return decodeCompileRes(rs.getString(1));
        }
      } catch (SQLException e) {
        throw new RuntimeException("CacheReal.loadCompile", e);
      }
    }
  }

  @Override
  public void close() {
    synchronized (dbLock) {
      try {
        conn.close();
      } catch (SQLException e) {
        throw new RuntimeException("CacheReal.close", e);
      }
      try {
        dirLock.release();
        lockChannel.close();
      } catch (IOException e) {
        throw new RuntimeException("CacheReal.close lock", e);
      }
    }
  }

  private static String serializeJson(JsonValue jv) {
    var sw = new StringWriter();
    var writer = new JsonWriter(sw);
    writer.setSerializeNulls(true);
    writer.setIndent("");
    try {
      Json.encodeValue(writer, jv, JsonValue.class, Json.Style.COMPACT);
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException("CacheReal.serializeJson", e);
    }
    return sw.toString();
  }

  private static JsonValue parseJson(String s) {
    try {
      return Json.deserialize(JsonValue.class, s);
    } catch (IOException e) {
      throw new RuntimeException("CacheReal.parseJson", e);
    }
  }

  private static String encodeArtifactBody(ArtifactBody body) {
    return serializeJson(ArtifactJson.encodeArtifactBody(body));
  }

  private static ArtifactBody decodeArtifactBody(String s) {
    var res = ArtifactJson.decodeArtifactBody(DUMMY_LOC, parseJson(s));
    return res.requireValue("CacheReal.decodeArtifactBody");
  }

  private static String encodeCompileRes(CompileRes res) {
    var obj = new TOrderedMap<String, JsonValue>();
    var logs = new TList<JsonValue>();
    for (var log : res.logs()) {
      var lm = new TOrderedMap<String, JsonValue>();
      lm.put("t", new JsonValueNumber(log.timestampMs()));
      lm.put("m", new JsonValueString(log.message()));
      logs.add(new JsonValueObject(lm));
    }
    obj.put("logs", new JsonValueArray(logs));
    switch (res.result()) {
      case CompileResResult.Ok ok -> obj.put("artifact", ArtifactJson.encodeArtifact(ok.artifact()));
      case CompileResResult.Err err -> obj.put("errors", encodeErrors(err.errors()));
    }
    return serializeJson(new JsonValueObject(obj));
  }

  private static JsonValue encodeErrors(TList<ExprErr> errors) {
    var arr = new TList<JsonValue>();
    for (var err : errors) {
      var em = new TOrderedMap<String, JsonValue>();
      em.put("s", new JsonValueNumber(err.loc().source().value()));
      em.put("e", new JsonValueNumber(err.loc().expr()));
      var msgs = new TList<JsonValue>();
      for (var sub : err.errors()) {
        switch (sub) {
          case ExprSubErr.Opaque o -> msgs.add(new JsonValueString(o.message()));
        }
      }
      em.put("m", new JsonValueArray(msgs));
      arr.add(new JsonValueObject(em));
    }
    return new JsonValueArray(arr);
  }

  private static TList<ExprErr> decodeErrors(JsonValue jv) {
    var out = new TList<ExprErr>();
    var arr = (JsonValueArray) jv;
    for (var e : arr.elements()) {
      var eo = (JsonValueObject) e;
      var source = (long) ((JsonValueNumber) eo.entries().get("s")).value();
      var expr = (long) ((JsonValueNumber) eo.entries().get("e")).value();
      var msgs = new TList<ExprSubErr>();
      for (var m : ((JsonValueArray) eo.entries().get("m")).elements()) {
        msgs.add(new ExprSubErr.Opaque(((JsonValueString) m).value()));
      }
      out.add(new ExprErr(new QExprId(new SourceId(source), expr), msgs));
    }
    return out;
  }

  private static CompileRes decodeCompileRes(String s) {
    var obj = (JsonValueObject) parseJson(s);
    var logsArr = (JsonValueArray) obj.entries().get("logs");
    var logs = new TList<LogEntry>();
    for (var e : logsArr.elements()) {
      var lo = (JsonValueObject) e;
      var t = (long) ((JsonValueNumber) lo.entries().get("t")).value();
      var m = ((JsonValueString) lo.entries().get("m")).value();
      logs.add(new LogEntry(t, m));
    }
    var errorsJson = obj.entries().getOrNull("errors");
    if (errorsJson != null) {
      return new CompileRes(logs, new CompileResResult.Err(decodeErrors(errorsJson)));
    }
    var artRes = ArtifactJson.decodeArtifact(DUMMY_LOC, obj.entries().get("artifact"));
    var art = artRes.requireValue("CacheReal.decodeCompileRes artifact");
    return new CompileRes(logs, new CompileResResult.Ok(art));
  }

  private static String encodeCompileIds(TList<CompileId> ids) {
    var arr = new TList<JsonValue>();
    for (var id : ids) {
      var m = new TOrderedMap<String, JsonValue>();
      m.put("s", new JsonValueNumber(id.importState().value()));
      m.put("i", new JsonValueNumber(id.source().value()));
      arr.add(new JsonValueObject(m));
    }
    return serializeJson(new JsonValueArray(arr));
  }

  private static TList<CompileId> decodeCompileIds(String s) {
    var out = new TList<CompileId>();
    if (s == null || s.isEmpty()) return out;
    var arr = (JsonValueArray) parseJson(s);
    for (var e : arr.elements()) {
      var o = (JsonValueObject) e;
      var s0 = (long) ((JsonValueNumber) o.entries().get("s")).value();
      var i0 = (long) ((JsonValueNumber) o.entries().get("i")).value();
      out.add(new CompileId(new ImportStateId(s0), new SourceId(i0)));
    }
    return out;
  }
}
