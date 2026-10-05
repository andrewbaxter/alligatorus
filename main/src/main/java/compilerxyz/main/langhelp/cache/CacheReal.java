package compilerxyz.main.langhelp.cache;

import com.github.luben.zstd.ZstdInputStream;
import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactJson;
import compilerxyz.main.artifact.Artifacts;
import compilerxyz.main.eval.CompileId;
import compilerxyz.main.eval.CompileRes;
import compilerxyz.main.eval.CompileResResult;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.ExprSubErr;
import compilerxyz.main.eval.ImportStateId;
import compilerxyz.main.eval.LogEntry;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.SourceId;
import compilerxyz.main.gen.codec.SourceModuleCodec;
import compilerxyz.main.langhelp.Compiles;
import compilerxyz.main.langhelp.Sources;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueArray;
import compilerxyz.main.safejson.JsonValueNumber;
import compilerxyz.main.safejson.JsonValueObject;
import compilerxyz.main.safejson.JsonValueString;
import compilerxyz.main.safejson.JsonValues;
import compilerxyz.main.syntax.HashValue;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import compilerxyz.prelude.util.TOrderedMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class CacheReal implements Sources, Artifacts, Compiles, AutoCloseable {
    private static final int SCHEMA_VERSION = 24;

    private final Connection conn;
    private final Object dbLock = new Object();
    private final FileChannel lockChannel;
    private final FileLock dirLock;
    private final HttpClient http =
            HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    private final TMap<String, SourceSlot> sourceMem = new TMap<>();
    private final ReentrantLock sourceMapLock = new ReentrantLock();
    private final TMap<ArtifactIdSource, ArtifactBody> artifactMem = new TMap<>();
    private final TMap<ArtifactIdSource, ArtifactBody> pendingArtifacts = new TMap<>();
    private final TMap<CompileId, CompileSlot> compileMem = new TMap<>();
    private final ReentrantLock compileMapLock = new ReentrantLock();

    private static final class CompileSlot {
        final ReentrantLock lock = new ReentrantLock();
        Opt<CompileRes> value = Opt.none();
    }

    private static final class SourceSlot {
        final ReentrantLock lock = new ReentrantLock();
        Opt<SourceResolved.Module> value = Opt.none();
    }

    public CacheReal() {
        var env = System.getenv("ALLIG_CACHE_DIR");
        Path dir;
        if (env != null && !env.isBlank()) {
            dir = Path.of(env);
        } else {
            var os = System.getProperty("os.name", "").toLowerCase();
            var home = System.getProperty("user.home");
            if (os.contains("win")) {
                var local = System.getenv("LOCALAPPDATA");
                dir = local != null && !local.isBlank() ? Path.of(local) : Path.of(home, "AppData", "Local");
            } else if (os.contains("mac")) {
                dir = Path.of(home, "Library", "Caches");
            } else {
                var xdg = System.getenv("XDG_CACHE_HOME");
                dir = xdg != null && !xdg.isBlank() ? Path.of(xdg) : Path.of(home, ".cache");
            }
            dir = dir.resolve("allig");
        }
        try {
            Class.forName("org.sqlite.JDBC");
            Files.createDirectories(dir);
            lockChannel = FileChannel.open(dir.resolve("lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock acquired;
            try {
                acquired = lockChannel.tryLock();
            } catch (OverlappingFileLockException e) {
                acquired = null;
            }
            if (acquired == null) {
                lockChannel.close();
                throw new RuntimeException("CacheReal: cache directory is already in use by another instance: " + dir);
            }
            dirLock = acquired;
            conn = DriverManager.getConnection("jdbc:sqlite:" + dir.resolve("cache.sqlite"));
            conn.setAutoCommit(true);
            configureConnection();
            initSchema();
        } catch (ClassNotFoundException | SQLException | IOException e) {
            throw new RuntimeException("CacheReal: failed to open cache database", e);
        }
    }

    public CacheReal(Path dir) {
        try {
            Class.forName("org.sqlite.JDBC");
            Files.createDirectories(dir);
            lockChannel = FileChannel.open(dir.resolve("lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock acquired;
            try {
                acquired = lockChannel.tryLock();
            } catch (OverlappingFileLockException e) {
                acquired = null;
            }
            if (acquired == null) {
                lockChannel.close();
                throw new RuntimeException("CacheReal: cache directory is already in use by another instance: " + dir);
            }
            dirLock = acquired;
            conn = DriverManager.getConnection("jdbc:sqlite:" + dir.resolve("cache.sqlite"));
            conn.setAutoCommit(true);
            configureConnection();
            initSchema();
        } catch (ClassNotFoundException | SQLException | IOException e) {
            throw new RuntimeException("CacheReal: failed to open cache database", e);
        }
    }

    private void configureConnection() throws SQLException {
        try (var st = conn.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");
        }
    }

    private void initSchema() throws SQLException {
        synchronized (dbLock) {
            var existing = readSchemaVersion();
            if (existing.optIsSome() && existing.optUnwrap() != SCHEMA_VERSION) {
                System.err.println("warning: allig cache schema version "
                        + existing.optUnwrap()
                        + " does not match expected "
                        + SCHEMA_VERSION
                        + "; rebuilding cache");
                try (var st = conn.createStatement()) {
                    st.executeUpdate("DROP TABLE IF EXISTS schema_version");
                    st.executeUpdate("DROP TABLE IF EXISTS artifact_seq");
                    st.executeUpdate("DROP TABLE IF EXISTS source_seq");
                    st.executeUpdate("DROP TABLE IF EXISTS sources");
                    st.executeUpdate("DROP TABLE IF EXISTS artifact_ids");
                    st.executeUpdate("DROP TABLE IF EXISTS source_ids");
                    st.executeUpdate("DROP TABLE IF EXISTS remote_bytes");
                    st.executeUpdate("DROP TABLE IF EXISTS artifacts");
                    st.executeUpdate("DROP TABLE IF EXISTS compiles");
                }
            }
            try (var st = conn.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER NOT NULL)");
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS artifact_ids ("
                                + "idx INTEGER PRIMARY KEY AUTOINCREMENT, import_state INTEGER NOT NULL, source INTEGER NOT NULL)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS source_ids ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, spec_key TEXT NOT NULL UNIQUE)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS remote_bytes (url TEXT PRIMARY KEY, bytes BLOB NOT NULL)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS artifacts ("
                        + "import_state INTEGER NOT NULL, source INTEGER NOT NULL, idx INTEGER NOT NULL,"
                        + " body TEXT NOT NULL, PRIMARY KEY (import_state, source, idx))");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS compiles ("
                        + "import_state INTEGER NOT NULL, source INTEGER NOT NULL, result TEXT NOT NULL,"
                        + " local_path TEXT, local_mtime INTEGER, local_deps TEXT, local_data TEXT,"
                        + " PRIMARY KEY (import_state, source))");
            }
            if (readSchemaVersion().optIsNone()) {
                try (var ins = conn.prepareStatement("INSERT INTO schema_version (version) VALUES (?)")) {
                    ins.setInt(1, SCHEMA_VERSION);
                    ins.executeUpdate();
                }
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

    private SourceId sourceIdFor(String specKey) {
        synchronized (dbLock) {
            try {
                try (var ps = conn.prepareStatement("INSERT OR IGNORE INTO source_ids (spec_key) VALUES (?)")) {
                    ps.setString(1, specKey);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("SELECT id FROM source_ids WHERE spec_key = ?")) {
                    ps.setString(1, specKey);
                    try (var rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalStateException("source id missing after insert: " + specKey);
                        return new SourceId(rs.getLong(1));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("CacheReal.sourceIdFor", e);
            }
        }
    }

    private Result<SourceResolved.Module, String> sourceShared(
            String specKey, Opt<Long> mtime, Supplier<Result<SourceResolved.Module, String>> parse) {
        SourceSlot slot;
        sourceMapLock.lock();
        try {
            slot = sourceMem.computeIfAbsentT(specKey, k -> new SourceSlot());
        } finally {
            sourceMapLock.unlock();
        }
        slot.lock.lock();
        try {
            if (slot.value.optIsSome()) {
                var cached = slot.value.optUnwrap();
                if (mtime.optIsNone() || cached.local().optUnwrap().timestampMs() == mtime.optUnwrap()) {
                    return Result.ok(cached);
                }
            }
            var parsed = parse.get();
            switch (parsed) {
                case Result.Ok<SourceResolved.Module, String> ok -> slot.value = Opt.some(ok.value());
                case Result.Err<SourceResolved.Module, String> err -> {}
            }
            return parsed;
        } finally {
            slot.lock.unlock();
        }
    }

    private static Result<SourceResolved, String> widen(Result<SourceResolved.Module, String> res) {
        return switch (res) {
            case Result.Ok<SourceResolved.Module, String> ok -> Result.ok(ok.value());
            case Result.Err<SourceResolved.Module, String> err -> Result.err(err.error());
        };
    }

    @Override
    public Result<SourceResolved, String> sourcesGet(SourceSpecAbsolute spec) {
        switch (spec) {
            case SourceSpecAbsolute.Virtual v -> {
                return Result.err("virtual sources are not supported by the real cache: " + v.name());
            }
            case SourceSpecAbsolute.Local l -> {
                var file = Path.of(l.path());
                if (!Files.isRegularFile(file)) {
                    return Result.err("local source not found: " + l.path());
                }
                long mtime;
                try {
                    mtime = Files.getLastModifiedTime(file).toMillis();
                } catch (IOException e) {
                    return Result.err("failed to read local source " + l.path() + ": " + e.getMessage());
                }
                var specKey = l.sourceSpecAbsoluteSpecKey();
                return widen(sourceShared(specKey, Opt.some(mtime), () -> {
                    byte[] raw;
                    try {
                        raw = Files.readAllBytes(file);
                    } catch (IOException e) {
                        return Result.err("failed to read local source " + l.path() + ": " + e.getMessage());
                    }
                    SourceModule module;
                    try {
                        module = SourceModuleCodec.deserialize(moduleText(raw));
                    } catch (IOException | RuntimeException e) {
                        return Result.err("failed to parse local source " + l.path() + ": " + e.getMessage());
                    }
                    var id = sourceIdFor(specKey);
                    return Result.ok(
                            new SourceResolved.Module(id, module, Opt.some(new Compiles.LocalMeta(l.path(), mtime))));
                }));
            }
            case SourceSpecAbsolute.Remote r -> {
                byte[] bytes;
                switch (remoteBytes(r.baseUrl(), r.hash())) {
                    case Result.Ok<byte[], String> ok -> bytes = ok.value();
                    case Result.Err<byte[], String> err -> {
                        return Result.err(err.error());
                    }
                }
                var isZip = isZip(bytes);
                if (r.subpath().isEmpty()) {
                    if (isZip) return Result.ok(new SourceResolved.Bundle(r));
                    var specKey = r.sourceSpecAbsoluteSpecKey();
                    return widen(sourceShared(specKey, Opt.none(), () -> {
                        SourceModule module;
                        try {
                            module = SourceModuleCodec.deserialize(moduleText(bytes));
                        } catch (IOException | RuntimeException e) {
                            return Result.err("failed to parse remote source " + r.baseUrl() + ": " + e.getMessage());
                        }
                        return Result.ok(new SourceResolved.Module(sourceIdFor(specKey), module, Opt.none()));
                    }));
                }
                if (!isZip) {
                    return Result.err("remote source is not a bundle but a subpath was requested: " + r.subpath());
                }
                byte[] entryBytes;
                switch (zipEntryBytes(bytes, r.subpath())) {
                    case Result.Ok<byte[], String> ok -> entryBytes = ok.value();
                    case Result.Err<byte[], String> err -> {
                        return Result.err(err.error());
                    }
                }
                var entryKey = r.sourceSpecAbsoluteSpecKey();
                return widen(sourceShared(entryKey, Opt.none(), () -> {
                    SourceModule module;
                    try {
                        module = SourceModuleCodec.deserialize(moduleText(entryBytes));
                    } catch (IOException | RuntimeException e) {
                        return Result.err("failed to parse remote source " + r.baseUrl() + ": " + e.getMessage());
                    }
                    return Result.ok(new SourceResolved.Module(sourceIdFor(entryKey), module, Opt.none()));
                }));
            }
        }
    }

    private Result<byte[], String> remoteBytes(String url, HashValue hash) {
        byte[] cached;
        synchronized (dbLock) {
            try (var ps = conn.prepareStatement("SELECT bytes FROM remote_bytes WHERE url = ?")) {
                ps.setString(1, url);
                try (var rs = ps.executeQuery()) {
                    cached = rs.next() ? rs.getBytes(1) : null;
                }
            } catch (SQLException e) {
                throw new RuntimeException("CacheReal.loadRemoteBytes", e);
            }
        }
        Result<byte[], String> bytesRes;
        if (cached != null) {
            bytesRes = verifyHash(url, cached, hash);
        } else {
            byte[] fetched = null;
            Result<byte[], String> fetchResult;
            try {
                var response = http.send(
                        HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200) {
                    fetchResult = Result.err("remote fetch failed (" + response.statusCode() + "): " + url);
                } else {
                    fetched = response.body();
                    fetchResult = verifyHash(url, fetched, hash);
                }
            } catch (IOException e) {
                fetchResult = Result.err("remote fetch failed: " + url + ": " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fetchResult = Result.err("remote fetch interrupted: " + url);
            }
            if (fetched != null
                    && switch (fetchResult) {
                        case Result.Ok<byte[], String> ok -> true;
                        case Result.Err<byte[], String> err -> false;
                    }) {
                synchronized (dbLock) {
                    try (var ps =
                            conn.prepareStatement("INSERT OR REPLACE INTO remote_bytes (url, bytes) VALUES (?, ?)")) {
                        ps.setString(1, url);
                        ps.setBytes(2, fetched);
                        ps.executeUpdate();
                    } catch (SQLException e) {
                        throw new RuntimeException("CacheReal.storeRemoteBytes", e);
                    }
                }
            }
            bytesRes = fetchResult;
        }
        return bytesRes;
    }

    private static Result<byte[], String> zipEntryBytes(byte[] bundle, String subpath) {
        try (var zin = new ZipInputStream(new ByteArrayInputStream(bundle))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (entry.getName().equals(subpath)) {
                    return Result.ok(zin.readAllBytes());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("CacheReal.extractZipEntry", e);
        }
        return Result.err("bundle entry not found: " + subpath);
    }

    private static boolean isZstd(byte[] bytes) {
        return bytes.length >= 4
                && bytes[0] == (byte) 0x28
                && bytes[1] == (byte) 0xB5
                && bytes[2] == (byte) 0x2F
                && bytes[3] == (byte) 0xFD;
    }

    private static String moduleText(byte[] bytes) throws IOException {
        if (!isZstd(bytes)) return new String(bytes, StandardCharsets.UTF_8);
        try (var in = new ZstdInputStream(new ByteArrayInputStream(bytes))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static boolean isZip(byte[] bytes) {
        return bytes.length >= 4 && bytes[0] == 'P' && bytes[1] == 'K' && bytes[2] == 3 && bytes[3] == 4;
    }

    @Override
    public Result<SourceData, String> sourcesGetData(SourceSpecAbsolute spec) {
        switch (spec) {
            case SourceSpecAbsolute.Virtual v -> {
                return Result.err("virtual sources are not supported by the real cache: " + v.name());
            }
            case SourceSpecAbsolute.Local l -> {
                var file = Path.of(l.path());
                if (!Files.isRegularFile(file)) {
                    return Result.err("local file not found: " + l.path());
                }
                try {
                    var bytes = Files.readAllBytes(file);
                    var mtime = Files.getLastModifiedTime(file).toMillis();
                    return Result.ok(new SourceData(bytes, Opt.some(new Compiles.LocalMeta(l.path(), mtime))));
                } catch (IOException e) {
                    return Result.err("failed to read local file " + l.path() + ": " + e.getMessage());
                }
            }
            case SourceSpecAbsolute.Remote r -> {
                byte[] bytes;
                switch (remoteBytes(r.baseUrl(), r.hash())) {
                    case Result.Ok<byte[], String> ok -> bytes = ok.value();
                    case Result.Err<byte[], String> err -> {
                        return Result.err(err.error());
                    }
                }
                if (r.subpath().isEmpty()) {
                    return Result.ok(new SourceData(bytes, Opt.none()));
                }
                if (!isZip(bytes)) {
                    return Result.err("remote source is not a bundle but a subpath was requested: " + r.subpath());
                }
                return switch (zipEntryBytes(bytes, r.subpath())) {
                    case Result.Ok<byte[], String> ok -> Result.ok(new SourceData(ok.value(), Opt.none()));
                    case Result.Err<byte[], String> err -> Result.err(err.error());
                };
            }
        }
    }

    private static Result<byte[], String> verifyHash(String url, byte[] bytes, HashValue hash) {
        byte[] actual;
        try {
            actual = MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
        switch (hash) {
            case HashValue.Sha256 expected -> {
                if (!Arrays.equals(actual, expected.digest())) {
                    return Result.err("remote hash mismatch for " + url + ": expected " + hash.hashValueHashKey()
                            + ", got sha256:" + HexFormat.of().formatHex(actual));
                }
            }
        }
        return Result.ok(bytes);
    }

    @Override
    public synchronized Result<ArtifactBody, String> artifactsGet(ArtifactIdSource id) {
        var memOpt = artifactMem.getOpt(id);
        if (memOpt.optIsSome()) {
            var mem = memOpt.optUnwrap();
            return Result.ok(mem);
        }
        synchronized (dbLock) {
            try (var ps = conn.prepareStatement(
                    "SELECT body FROM artifacts WHERE import_state = ? AND source = ? AND idx = ?")) {
                ps.setLong(1, id.compileId().importState().value());
                ps.setLong(2, id.compileId().source().value());
                ps.setLong(3, id.index());
                try (var rs = ps.executeQuery()) {
                    if (!rs.next()) return Result.err("Unknown artifact: " + id);
                    var body = ArtifactJson.decodeArtifactBody(parseJson(rs.getString(1)));
                    artifactMem.put(id, body);
                    return Result.ok(body);
                }
            } catch (SQLException e) {
                throw new RuntimeException("CacheReal.artifactsGet", e);
            }
        }
    }

    @Override
    public ArtifactIdSource artifactsReserveId(CompileId compileId) {
        synchronized (dbLock) {
            try (var ps = conn.prepareStatement(
                    "INSERT INTO artifact_ids (import_state, source) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, compileId.importState().value());
                ps.setLong(2, compileId.source().value());
                ps.executeUpdate();
                try (var rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) throw new IllegalStateException("artifact id reservation returned no key");
                    return new ArtifactIdSource(compileId, rs.getLong(1));
                }
            } catch (SQLException e) {
                throw new RuntimeException("CacheReal.artifactsReserveId", e);
            }
        }
    }

    @Override
    public synchronized void artifactsSet(ArtifactIdSource id, ArtifactBody body) {
        artifactMem.put(id, body);
        pendingArtifacts.put(id, body);
    }

    @Override
    public void compilesPrepare(CompileId root) {
        evictIfStale(root);
    }

    private boolean evictIfStale(CompileId id) {
        synchronized (dbLock) {
            return staleWalk(id, new HashMap<>(), new HashSet<>());
        }
    }

    private boolean staleWalk(CompileId id, Map<CompileId, Boolean> memo, Set<CompileId> visiting) {
        var cached = memo.get(id);
        if (cached != null) return cached;
        if (!visiting.add(id)) return false;
        CompileRow row;
        try (var ps = conn.prepareStatement(
                "SELECT local_path, local_mtime, local_deps, local_data FROM compiles WHERE import_state = ? AND source = ?")) {
            ps.setLong(1, id.importState().value());
            ps.setLong(2, id.source().value());
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    row = null;
                } else {
                    var localPath = rs.getString(1);
                    var localMtime = rs.getLong(2);
                    TList<CompileId> deps;
                    {
                        var s = rs.getString(3);
                        deps = new TList<>();
                        if (s != null && !s.isEmpty()) {
                            var arr = (JsonValueArray) parseJson(s);
                            for (var e : arr.elements()) {
                                var o = (JsonValueObject) e;
                                var s0 = (long) ((JsonValueNumber)
                                                o.entries().getOpt("s").optUnwrap())
                                        .value();
                                var i0 = (long) ((JsonValueNumber)
                                                o.entries().getOpt("i").optUnwrap())
                                        .value();
                                deps.add(new CompileId(new ImportStateId(s0), new SourceId(i0)));
                            }
                        }
                    }
                    var data = new TList<Compiles.LocalMeta>();
                    {
                        var s = rs.getString(4);
                        if (s != null && !s.isEmpty()) {
                            var arr = (JsonValueArray) parseJson(s);
                            for (var e : arr.elements()) {
                                var o = (JsonValueObject) e;
                                var path = ((JsonValueString)
                                                o.entries().getOpt("p").optUnwrap())
                                        .value();
                                var t = (long) ((JsonValueNumber)
                                                o.entries().getOpt("t").optUnwrap())
                                        .value();
                                data.add(new Compiles.LocalMeta(path, t));
                            }
                        }
                    }
                    row = new CompileRow(localPath, localMtime, deps, data);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("CacheReal.loadCompileRow", e);
        }
        if (row == null) {
            memo.put(id, false);
            visiting.remove(id);
            return false;
        }
        var stale = false;
        if (row.localPath != null && mtimeChanged(row.localPath, row.localMtime)) stale = true;
        for (var data : row.localData) {
            if (mtimeChanged(data.path(), data.timestampMs())) stale = true;
        }
        for (var dep : row.localDeps) {
            if (staleWalk(dep, memo, visiting)) stale = true;
        }
        if (stale) {
            try (var ps = conn.prepareStatement("DELETE FROM compiles WHERE import_state = ? AND source = ?")) {
                ps.setLong(1, id.importState().value());
                ps.setLong(2, id.source().value());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("CacheReal.deleteCompile", e);
            }
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

    private static boolean mtimeChanged(String path, long recorded) {
        var file = Path.of(path);
        if (!Files.isRegularFile(file)) return true;
        try {
            return Files.getLastModifiedTime(file).toMillis() != recorded;
        } catch (IOException e) {
            return true;
        }
    }

    private record CompileRow(
            String localPath, long localMtime, TList<CompileId> localDeps, TList<Compiles.LocalMeta> localData) {}

    @Override
    public CompileRes compilesGetOrCompute(CompileId id, Supplier<CompileProduct> compute) {
        CompileSlot slot;
        compileMapLock.lock();
        try {
            slot = compileMem.computeIfAbsentT(id, k -> new CompileSlot());
        } finally {
            compileMapLock.unlock();
        }
        slot.lock.lock();
        try {
            if (slot.value.optIsSome()) return slot.value.optUnwrap();
            evictIfStale(id);
            Opt<CompileRes> fromDb;
            {
                synchronized (dbLock) {
                    try (var ps = conn.prepareStatement(
                            "SELECT result FROM compiles WHERE import_state = ? AND source = ?")) {
                        ps.setLong(1, id.importState().value());
                        ps.setLong(2, id.source().value());
                        try (var rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                fromDb = Opt.none();
                            } else {
                                var obj = (JsonValueObject) parseJson(rs.getString(1));
                                var logsArr = (JsonValueArray)
                                        obj.entries().getOpt("logs").optUnwrap();
                                var logs = new TList<LogEntry>();
                                for (var e : logsArr.elements()) {
                                    var lo = (JsonValueObject) e;
                                    var t = (long) ((JsonValueNumber)
                                                    lo.entries().getOpt("t").optUnwrap())
                                            .value();
                                    var m = ((JsonValueString)
                                                    lo.entries().getOpt("m").optUnwrap())
                                            .value();
                                    logs.add(new LogEntry(t, m));
                                }
                                var errorsJsonOpt = obj.entries().getOpt("errors");
                                if (errorsJsonOpt.optIsSome()) {
                                    var errorsJson = errorsJsonOpt.optUnwrap();
                                    var out = new TList<ExprErr>();
                                    var arr = (JsonValueArray) errorsJson;
                                    for (var e : arr.elements()) {
                                        var eo = (JsonValueObject) e;
                                        var source = (long) ((JsonValueNumber)
                                                        eo.entries().getOpt("s").optUnwrap())
                                                .value();
                                        var exprJson = eo.entries().getOpt("e");
                                        Opt<Long> expr = exprJson.optIsSome()
                                                ? Opt.some((long) ((JsonValueNumber) exprJson.optUnwrap()).value())
                                                : Opt.none();
                                        var msgs = new TList<ExprSubErr>();
                                        for (var mv : ((JsonValueArray)
                                                        eo.entries().getOpt("m").optUnwrap())
                                                .elements()) {
                                            msgs.add(new ExprSubErr.Opaque(((JsonValueString) mv).value()));
                                        }
                                        out.add(new ExprErr(new QExprId(new SourceId(source), expr), msgs));
                                    }
                                    fromDb = Opt.some(new CompileRes(logs, new CompileResResult.Err(out)));
                                } else {
                                    var artifactJson = obj.entries().getOpt("artifact");
                                    Opt<Artifact> art = Opt.none();
                                    if (artifactJson.optIsSome()) {
                                        art = Opt.some(ArtifactJson.decodeArtifact(artifactJson.optUnwrap()));
                                    }
                                    fromDb = Opt.some(new CompileRes(logs, new CompileResResult.Ok(art)));
                                }
                            }
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException("CacheReal.loadCompile", e);
                    }
                }
            }
            if (fromDb.optIsSome()) {
                slot.value = fromDb;
                return fromDb.optUnwrap();
            }
            var product = compute.get();
            slot.value = Opt.some(product.res());
            {
                TMap<ArtifactIdSource, ArtifactBody> artifacts;
                synchronized (this) {
                    artifacts = new TMap<ArtifactIdSource, ArtifactBody>();
                    for (var e : pendingArtifacts.entrySet()) artifacts.put(e.getKey(), e.getValue());
                    pendingArtifacts.clear();
                }
                synchronized (dbLock) {
                    try {
                        conn.setAutoCommit(false);
                        try {
                            try (var ps = conn.prepareStatement(
                                    "INSERT OR REPLACE INTO artifacts (import_state, source, idx, body) VALUES (?, ?, ?, ?)")) {
                                for (var e : artifacts.entrySet()) {
                                    var aid = e.getKey();
                                    ps.setLong(1, aid.compileId().importState().value());
                                    ps.setLong(2, aid.compileId().source().value());
                                    ps.setLong(3, aid.index());
                                    ps.setString(
                                            4, JsonValues.serialize(ArtifactJson.encodeArtifactBody(e.getValue())));
                                    ps.addBatch();
                                }
                                ps.executeBatch();
                            }
                            try (var ps = conn.prepareStatement("INSERT OR REPLACE INTO compiles"
                                    + " (import_state, source, result, local_path, local_mtime, local_deps, local_data)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                                ps.setLong(1, id.importState().value());
                                ps.setLong(2, id.source().value());
                                {
                                    var res = product.res();
                                    var obj = new TOrderedMap<String, JsonValue>();
                                    var logsArr = new TList<JsonValue>();
                                    for (var log : res.logs()) {
                                        var lm = new TOrderedMap<String, JsonValue>();
                                        lm.put("t", new JsonValueNumber(log.timestampMs()));
                                        lm.put("m", new JsonValueString(log.message()));
                                        logsArr.add(new JsonValueObject(lm));
                                    }
                                    obj.put("logs", new JsonValueArray(logsArr));
                                    switch (res.result()) {
                                        case CompileResResult.Ok ok -> {
                                            if (ok.artifact().optIsSome()) {
                                                obj.put(
                                                        "artifact",
                                                        ArtifactJson.encodeArtifact(
                                                                ok.artifact().optUnwrap()));
                                            }
                                        }
                                        case CompileResResult.Err err -> {
                                            var errArr = new TList<JsonValue>();
                                            for (var err2 : err.errors()) {
                                                var em = new TOrderedMap<String, JsonValue>();
                                                em.put(
                                                        "s",
                                                        new JsonValueNumber(err2.loc()
                                                                .source()
                                                                .value()));
                                                if (err2.loc().expr().optIsSome()) {
                                                    em.put(
                                                            "e",
                                                            new JsonValueNumber(err2.loc()
                                                                    .expr()
                                                                    .optUnwrap()));
                                                }
                                                var msgs = new TList<JsonValue>();
                                                for (var sub : err2.errors()) {
                                                    switch (sub) {
                                                        case ExprSubErr.Opaque o ->
                                                            msgs.add(new JsonValueString(o.message()));
                                                    }
                                                }
                                                em.put("m", new JsonValueArray(msgs));
                                                errArr.add(new JsonValueObject(em));
                                            }
                                            obj.put("errors", new JsonValueArray(errArr));
                                        }
                                    }
                                    ps.setString(3, JsonValues.serialize(new JsonValueObject(obj)));
                                }
                                if (product.local().optIsSome()) {
                                    var localMeta = product.local().optUnwrap();
                                    ps.setString(4, localMeta.path());
                                    ps.setLong(5, localMeta.timestampMs());
                                } else {
                                    ps.setNull(4, Types.VARCHAR);
                                    ps.setNull(5, Types.INTEGER);
                                }
                                {
                                    var arr = new TList<JsonValue>();
                                    for (var depId : product.localDeps()) {
                                        var mv = new TOrderedMap<String, JsonValue>();
                                        mv.put(
                                                "s",
                                                new JsonValueNumber(
                                                        depId.importState().value()));
                                        mv.put(
                                                "i",
                                                new JsonValueNumber(
                                                        depId.source().value()));
                                        arr.add(new JsonValueObject(mv));
                                    }
                                    ps.setString(6, JsonValues.serialize(new JsonValueArray(arr)));
                                }
                                {
                                    var arr = new TList<JsonValue>();
                                    for (var data : product.localData()) {
                                        var mv = new TOrderedMap<String, JsonValue>();
                                        mv.put("p", new JsonValueString(data.path()));
                                        mv.put("t", new JsonValueNumber(data.timestampMs()));
                                        arr.add(new JsonValueObject(mv));
                                    }
                                    ps.setString(7, JsonValues.serialize(new JsonValueArray(arr)));
                                }
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
            return slot.value.optUnwrap();
        } finally {
            slot.lock.unlock();
        }
    }

    @Override
    public TList<CompileRes> compilesResults() {
        compileMapLock.lock();
        try {
            var out = new TList<CompileRes>();
            for (var slot : compileMem.values()) {
                if (slot.value.optIsSome()) out.add(slot.value.optUnwrap());
            }
            return out;
        } finally {
            compileMapLock.unlock();
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

    private static JsonValue parseJson(String s) {
        try {
            return JsonValues.parse(s);
        } catch (IOException e) {
            throw new RuntimeException("CacheReal.parseJson", e);
        }
    }
}
