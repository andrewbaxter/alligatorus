package compilerxyz.main.langhelp.cache;

import com.github.luben.zstd.ZstdInputStream;
import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactJson;
import compilerxyz.main.artifact.Artifacts;
import compilerxyz.main.eval.ArtifactExprErr;
import compilerxyz.main.eval.CompileId;
import compilerxyz.main.eval.CompileRes;
import compilerxyz.main.eval.CompileResResult;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.ImportStateId;
import compilerxyz.main.eval.LogEntry;
import compilerxyz.main.eval.SourceId;
import compilerxyz.main.gen.codec.ArtifactExprErrCodec;
import compilerxyz.main.gen.codec.SourceModuleCodec;
import compilerxyz.main.langhelp.Compiles;
import compilerxyz.main.langhelp.Sources;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueArray;
import compilerxyz.main.safejson.JsonValueNumberInt;
import compilerxyz.main.safejson.JsonValueObject;
import compilerxyz.main.safejson.JsonValueString;
import compilerxyz.main.safejson.JsonValues;
import compilerxyz.main.syntax.HashValue;
import compilerxyz.main.syntax.RemoteOrigin;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
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
    private static final int SCHEMA_VERSION = 33;

    private final Connection conn;
    private final Object dbLock = new Object();
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

    public static Path cacheDirDefault() {
        var env = System.getenv("ALLIG_CACHE_DIR");
        if (env != null && !env.isBlank()) return Path.of(env);
        var os = System.getProperty("os.name", "").toLowerCase();
        var home = System.getProperty("user.home");
        Path dir;
        if (os.contains("win")) {
            var local = System.getenv("LOCALAPPDATA");
            dir = local != null && !local.isBlank() ? Path.of(local) : Path.of(home, "AppData", "Local");
        } else if (os.contains("mac")) {
            dir = Path.of(home, "Library", "Caches");
        } else {
            var xdg = System.getenv("XDG_CACHE_HOME");
            dir = xdg != null && !xdg.isBlank() ? Path.of(xdg) : Path.of(home, ".cache");
        }
        return dir.resolve("allig");
    }

    public CacheReal(Path dir) {
        try {
            Class.forName("org.sqlite.JDBC");
            Files.createDirectories(dir);
            conn = DriverManager.getConnection("jdbc:sqlite:" + dir.resolve("cache.sqlite"));
            conn.setAutoCommit(true);
            try (var st = conn.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("PRAGMA synchronous=NORMAL");
            }
            var existing = readSchemaVersion();
            if (existing.optIsSome() && existing.optUnwrap() != SCHEMA_VERSION) {
                System.err.println("cache cleared: schema version " + existing.optUnwrap() + " -> " + SCHEMA_VERSION);
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
                    st.executeUpdate("DROP TABLE IF EXISTS compile_deps");
                    st.executeUpdate("DROP TABLE IF EXISTS compile_data");
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
                        + " local_path TEXT, local_mtime INTEGER,"
                        + " PRIMARY KEY (import_state, source))");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS compile_data ("
                        + "import_state INTEGER NOT NULL, source INTEGER NOT NULL,"
                        + " path TEXT NOT NULL, mtime INTEGER NOT NULL,"
                        + " PRIMARY KEY (import_state, source, path))");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS compile_data_by_path ON compile_data (path)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS compile_deps ("
                        + "import_state INTEGER NOT NULL, source INTEGER NOT NULL,"
                        + " dep_import_state INTEGER NOT NULL, dep_source INTEGER NOT NULL,"
                        + " PRIMARY KEY (import_state, source, dep_import_state, dep_source))");
                st.executeUpdate(
                        "CREATE INDEX IF NOT EXISTS compile_deps_by_dep ON compile_deps (dep_source, dep_import_state)");
            }
            if (readSchemaVersion().optIsNone()) {
                try (var ins = conn.prepareStatement("INSERT INTO schema_version (version) VALUES (?)")) {
                    ins.setInt(1, SCHEMA_VERSION);
                    ins.executeUpdate();
                }
            }
        } catch (ClassNotFoundException | SQLException | IOException e) {
            throw new RuntimeException("CacheReal: failed to open cache database", e);
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
                if (Files.isDirectory(file)) {
                    return Result.ok(new SourceResolved.Bundle(l));
                }
                if (!Files.isRegularFile(file)) {
                    return Result.err("local source not found: " + l.path());
                }
                if (!isModulePath(l.path())) {
                    return Result.ok(new SourceResolved.File());
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
                switch (remoteBytes(r.origin())) {
                    case Result.Ok<byte[], String> ok -> bytes = ok.value();
                    case Result.Err<byte[], String> err -> {
                        return Result.err(err.error());
                    }
                }
                var isZip = isZip(bytes);
                if (r.subpath().isEmpty()) {
                    if (isZip) return Result.ok(new SourceResolved.Bundle(r));
                    if (!isModulePath(r.origin().remoteOriginDesc())) return Result.ok(new SourceResolved.File());
                    var specKey = r.sourceSpecAbsoluteSpecKey();
                    return widen(sourceShared(specKey, Opt.none(), () -> {
                        SourceModule module;
                        try {
                            module = SourceModuleCodec.deserialize(moduleText(bytes));
                        } catch (IOException | RuntimeException e) {
                            return Result.err("failed to parse remote source "
                                    + r.origin().remoteOriginDesc() + ": " + e.getMessage());
                        }
                        return Result.ok(new SourceResolved.Module(sourceIdFor(specKey), module, Opt.none()));
                    }));
                }
                if (!isZip) {
                    return Result.err("remote source is not a bundle but a subpath was requested: " + r.subpath());
                }
                boolean hasDir = false;
                var prefix = r.subpath().endsWith("/") ? r.subpath() : r.subpath() + "/";
                try (var zin = new ZipInputStream(new ByteArrayInputStream(bytes))) {
                    ZipEntry entry;
                    while ((entry = zin.getNextEntry()) != null) {
                        if (entry.getName().startsWith(prefix)) {
                            hasDir = true;
                            break;
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException("CacheReal.zipHasDirectory", e);
                }
                if (hasDir) {
                    return Result.ok(new SourceResolved.Bundle(r));
                }
                if (!isModulePath(r.subpath())) {
                    switch (zipEntryBytes(bytes, r.subpath())) {
                        case Result.Ok<byte[], String> ok -> {
                            return Result.ok(new SourceResolved.File());
                        }
                        case Result.Err<byte[], String> err -> {
                            return Result.err(err.error());
                        }
                    }
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
                        return Result.err("failed to parse remote source "
                                + r.origin().remoteOriginDesc() + ": " + e.getMessage());
                    }
                    return Result.ok(new SourceResolved.Module(sourceIdFor(entryKey), module, Opt.none()));
                }));
            }
        }
    }

    public static boolean isModulePath(String path) {
        return path.endsWith(".at") || path.endsWith(".atz");
    }

    private Result<byte[], String> remoteBytes(RemoteOrigin origin) {
        var key = origin.remoteOriginKey();
        byte[] cached;
        synchronized (dbLock) {
            try (var ps = conn.prepareStatement("SELECT bytes FROM remote_bytes WHERE url = ?")) {
                ps.setString(1, key);
                try (var rs = ps.executeQuery()) {
                    cached = rs.next() ? rs.getBytes(1) : null;
                }
            } catch (SQLException e) {
                throw new RuntimeException("CacheReal.loadRemoteBytes", e);
            }
        }
        if (cached != null) {
            return switch (origin) {
                case RemoteOrigin.Http h -> verifyHash(h.url(), cached, h.hash());
                case RemoteOrigin.Git g -> Result.ok(cached);
            };
        }
        System.err.println("pull start " + origin.remoteOriginDesc());
        var pullStartMs = System.currentTimeMillis();
        Result<byte[], String> fetched =
                switch (origin) {
                    case RemoteOrigin.Http h -> {
                        var url = h.url();
                        try {
                            var response = http.send(
                                    HttpRequest.newBuilder(URI.create(url))
                                            .GET()
                                            .build(),
                                    HttpResponse.BodyHandlers.ofByteArray());
                            if (response.statusCode() != 200) {
                                yield Result.err("remote fetch failed (" + response.statusCode() + "): " + url);
                            }
                            yield verifyHash(url, response.body(), h.hash());
                        } catch (IOException e) {
                            yield Result.err("remote fetch failed: " + url + ": " + e.getMessage());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            yield Result.err("remote fetch interrupted: " + url);
                        }
                    }
                    case RemoteOrigin.Git g -> {
                        Path dir;
                        try {
                            dir = Files.createTempDirectory("allig-git");
                        } catch (IOException e) {
                            yield Result.err("git fetch: cannot create a work directory: " + e.getMessage());
                        }
                        try {
                            for (var argv : new String[][] {
                                {"git", "init", "-q", "."},
                                {"git", "fetch", "-q", "--depth", "1", g.url(), g.commit()},
                            }) {
                                Result<byte[], String> gitRes = gitOutput(dir, argv);
                                switch (gitRes) {
                                    case Result.Ok<byte[], String> ok -> {}
                                    case Result.Err<byte[], String> err -> {
                                        yield Result.err(err.error());
                                    }
                                }
                            }
                            var head = gitOutput(dir, "git", "rev-parse", "FETCH_HEAD");
                            switch (head) {
                                case Result.Ok<byte[], String> ok -> {
                                    var got = new String(ok.value(), StandardCharsets.UTF_8).trim();
                                    if (!got.equals(g.commit())) {
                                        yield Result.err(
                                                "git fetch of " + g.remoteOriginDesc() + " yielded commit " + got);
                                    }
                                }
                                case Result.Err<byte[], String> err -> {
                                    yield Result.err(err.error());
                                }
                            }
                            yield gitOutput(dir, "git", "archive", "--format=zip", "FETCH_HEAD");
                        } finally {
                            try (var walk = Files.walk(dir)) {
                                for (var p :
                                        walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
                            } catch (IOException e) {
                                throw new RuntimeException("CacheReal.gitArchive cleanup " + dir, e);
                            }
                        }
                    }
                };
        var pullEndMs = System.currentTimeMillis();
        System.err.println("pull end " + origin.remoteOriginDesc() + " " + logTime(pullStartMs) + " -> "
                + logTime(pullEndMs) + " (" + (pullEndMs - pullStartMs) + " ms)"
                + switch (fetched) {
                    case Result.Ok<byte[], String> ok -> "";
                    case Result.Err<byte[], String> err -> " failed";
                });
        switch (fetched) {
            case Result.Ok<byte[], String> ok -> {
                synchronized (dbLock) {
                    try (var ps =
                            conn.prepareStatement("INSERT OR REPLACE INTO remote_bytes (url, bytes) VALUES (?, ?)")) {
                        ps.setString(1, key);
                        ps.setBytes(2, ok.value());
                        ps.executeUpdate();
                    } catch (SQLException e) {
                        throw new RuntimeException("CacheReal.storeRemoteBytes", e);
                    }
                }
            }
            case Result.Err<byte[], String> err -> {}
        }
        return fetched;
    }

    private static Result<byte[], String> gitOutput(Path dir, String... argv) {
        try {
            var proc = new ProcessBuilder(argv)
                    .directory(dir.toFile())
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start();
            var out = proc.getInputStream().readAllBytes();
            var err = new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            var code = proc.waitFor();
            if (code != 0) return Result.err(String.join(" ", argv) + " failed (" + code + "): " + err.trim());
            return Result.ok(out);
        } catch (IOException e) {
            return Result.err(String.join(" ", argv) + " failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.err(String.join(" ", argv) + " interrupted");
        }
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

    private static String moduleText(byte[] bytes) throws IOException {
        var zstd = bytes.length >= 4
                && bytes[0] == (byte) 0x28
                && bytes[1] == (byte) 0xB5
                && bytes[2] == (byte) 0x2F
                && bytes[3] == (byte) 0xFD;
        if (!zstd) return new String(bytes, StandardCharsets.UTF_8);
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
                switch (remoteBytes(r.origin())) {
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
                "SELECT local_path, local_mtime FROM compiles WHERE import_state = ? AND source = ?")) {
            ps.setLong(1, id.importState().value());
            ps.setLong(2, id.source().value());
            try (var rs = ps.executeQuery()) {
                row = rs.next() ? new CompileRow(rs.getString(1), rs.getLong(2)) : null;
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
        if (row.localPath != null && mtimeChanged(row.localPath, row.localMtime)) {
            System.err.println("dirty: " + row.localPath);
            stale = true;
        }
        try (var ps =
                conn.prepareStatement("SELECT path, mtime FROM compile_data WHERE import_state = ? AND source = ?")) {
            ps.setLong(1, id.importState().value());
            ps.setLong(2, id.source().value());
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (mtimeChanged(rs.getString(1), rs.getLong(2))) {
                        System.err.println("dirty: " + rs.getString(1) + " (data of " + row.localPath + ")");
                        stale = true;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("CacheReal.compilesData", e);
        }
        for (var dep : compilesDependencies(id)) {
            if (staleWalk(dep, memo, visiting)) stale = true;
        }
        if (stale) evictLocked(id);
        memo.put(id, stale);
        visiting.remove(id);
        return stale;
    }

    public void compilesEvict(CompileId id) {
        synchronized (dbLock) {
            evictLocked(id);
        }
    }

    private void evictLocked(CompileId id) {
        try {
            try (var ps = conn.prepareStatement("DELETE FROM compiles WHERE import_state = ? AND source = ?")) {
                ps.setLong(1, id.importState().value());
                ps.setLong(2, id.source().value());
                ps.executeUpdate();
            }
            for (var table : new String[] {"compile_deps", "compile_data"}) {
                try (var ps =
                        conn.prepareStatement("DELETE FROM " + table + " WHERE import_state = ? AND source = ?")) {
                    ps.setLong(1, id.importState().value());
                    ps.setLong(2, id.source().value());
                    ps.executeUpdate();
                }
            }
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

    public TList<CompileId> compilesDependencies(CompileId id) {
        synchronized (dbLock) {
            try (var ps = conn.prepareStatement(
                    "SELECT dep_import_state, dep_source FROM compile_deps WHERE import_state = ? AND source = ?")) {
                ps.setLong(1, id.importState().value());
                ps.setLong(2, id.source().value());
                return compileIdRows(ps);
            } catch (SQLException e) {
                throw new RuntimeException("CacheReal.compilesDependencies", e);
            }
        }
    }

    public TList<CompileId> compilesDependents(SourceId source) {
        synchronized (dbLock) {
            try (var ps = conn.prepareStatement("SELECT import_state, source FROM compile_deps WHERE dep_source = ?")) {
                ps.setLong(1, source.value());
                return compileIdRows(ps);
            } catch (SQLException e) {
                throw new RuntimeException("CacheReal.compilesDependents", e);
            }
        }
    }

    public TList<CompileId> compilesDataUsers(String path) {
        synchronized (dbLock) {
            try (var ps = conn.prepareStatement("SELECT import_state, source FROM compile_data WHERE path = ?")) {
                ps.setString(1, path);
                return compileIdRows(ps);
            } catch (SQLException e) {
                throw new RuntimeException("CacheReal.compilesDataUsers", e);
            }
        }
    }

    public TList<CompileId> compilesUnitsOf(SourceId source) {
        synchronized (dbLock) {
            try (var ps = conn.prepareStatement("SELECT import_state, source FROM compiles WHERE source = ?")) {
                ps.setLong(1, source.value());
                return compileIdRows(ps);
            } catch (SQLException e) {
                throw new RuntimeException("CacheReal.compilesUnitsOf", e);
            }
        }
    }

    private static TList<CompileId> compileIdRows(PreparedStatement ps) throws SQLException {
        var out = new TList<CompileId>();
        try (var rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new CompileId(new ImportStateId(rs.getLong(1)), new SourceId(rs.getLong(2))));
            }
        }
        return out;
    }

    public Opt<CompileRes> compilesGetStored(CompileId id) {
        synchronized (dbLock) {
            try (var ps = conn.prepareStatement("SELECT result FROM compiles WHERE import_state = ? AND source = ?")) {
                ps.setLong(1, id.importState().value());
                ps.setLong(2, id.source().value());
                try (var rs = ps.executeQuery()) {
                    if (!rs.next()) return Opt.none();
                    var obj = (JsonValueObject) parseJson(rs.getString(1));
                    var startMs =
                            ((JsonValueNumberInt) obj.entries().getOpt("start").optUnwrap()).value();
                    var endMs =
                            ((JsonValueNumberInt) obj.entries().getOpt("end").optUnwrap()).value();
                    var logsArr = (JsonValueArray) obj.entries().getOpt("logs").optUnwrap();
                    var logs = new TList<LogEntry>();
                    for (var e : logsArr.elements()) {
                        var lo = (JsonValueObject) e;
                        var t = ((JsonValueNumberInt) lo.entries().getOpt("t").optUnwrap()).value();
                        var m = ((JsonValueString) lo.entries().getOpt("m").optUnwrap()).value();
                        logs.add(new LogEntry(t, m));
                    }
                    var errorsJsonOpt = obj.entries().getOpt("errors");
                    if (errorsJsonOpt.optIsSome()) {
                        var out = new TList<ExprErr>();
                        for (var e : ((JsonValueArray) errorsJsonOpt.optUnwrap()).elements()) {
                            out.add(ArtifactExprErrCodec.fromValue(e).toExprErr());
                        }
                        return Opt.some(new CompileRes(startMs, endMs, logs, new CompileResResult.Err(out)));
                    }
                    var artifactJson = obj.entries().getOpt("artifact");
                    Opt<Artifact> art = Opt.none();
                    if (artifactJson.optIsSome()) {
                        art = Opt.some(ArtifactJson.decodeArtifact(artifactJson.optUnwrap()));
                    }
                    return Opt.some(new CompileRes(startMs, endMs, logs, new CompileResResult.Ok(art)));
                }
            } catch (SQLException e) {
                throw new RuntimeException("CacheReal.compilesGetStored", e);
            }
        }
    }

    public SourceId sourcesIdFor(SourceSpecAbsolute spec) {
        return sourceIdFor(spec.sourceSpecAbsoluteSpecKey());
    }

    public Opt<SourceSpecAbsolute> sourcesSpecOf(SourceId id) {
        synchronized (dbLock) {
            try (var ps = conn.prepareStatement("SELECT spec_key FROM source_ids WHERE id = ?")) {
                ps.setLong(1, id.value());
                try (var rs = ps.executeQuery()) {
                    if (!rs.next()) return Opt.none();
                    return Opt.some(SourceSpecAbsolute.sourceSpecAbsoluteParseKey(rs.getString(1)));
                }
            } catch (SQLException e) {
                throw new RuntimeException("CacheReal.sourcesSpecOf", e);
            }
        }
    }

    public Result<String, String> sourcesGetText(SourceSpecAbsolute spec) {
        return switch (sourcesGetData(spec)) {
            case Result.Err<SourceData, String> err -> Result.err(err.error());
            case Result.Ok<SourceData, String> ok -> {
                try {
                    yield Result.ok(moduleText(ok.value().bytes()));
                } catch (IOException e) {
                    yield Result.err(
                            "failed to decompress " + spec.sourceSpecAbsoluteSpecKey() + ": " + e.getMessage());
                }
            }
        };
    }

    private static String logTime(long ms) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(ms));
    }

    private static boolean mtimeChanged(String path, long recorded) {
        var file = Path.of(path);
        if (recorded == Compiles.LocalMeta.ABSENT) return Files.exists(file);
        if (!Files.isRegularFile(file)) return true;
        try {
            return Files.getLastModifiedTime(file).toMillis() != recorded;
        } catch (IOException e) {
            return true;
        }
    }

    private record CompileRow(String localPath, long localMtime) {}

    @Override
    public CompileRes compilesGetOrCompute(CompileId id, SourceSpecAbsolute spec, Supplier<CompileProduct> compute) {
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
            var fromDb = compilesGetStored(id);
            if (fromDb.optIsSome()) {
                slot.value = fromDb;
                return fromDb.optUnwrap();
            }
            var module = spec.sourceSpecAbsoluteSpecKey() + " (import state "
                    + id.importState().value() + ")";
            System.err.println("compile start " + module);
            var product = compute.get();
            for (var log : product.res().logs()) {
                System.err.println("log " + module + ": " + log.message());
            }
            System.err.println("compile end " + module + " "
                    + logTime(product.res().startMs())
                    + " -> "
                    + logTime(product.res().endMs())
                    + " (" + (product.res().endMs() - product.res().startMs()) + " ms)");
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
                                    + " (import_state, source, result, local_path, local_mtime)"
                                    + " VALUES (?, ?, ?, ?, ?)")) {
                                ps.setLong(1, id.importState().value());
                                ps.setLong(2, id.source().value());
                                {
                                    var res = product.res();
                                    var obj = new TOrderedMap<String, JsonValue>();
                                    obj.put("start", new JsonValueNumberInt(res.startMs()));
                                    obj.put("end", new JsonValueNumberInt(res.endMs()));
                                    var logsArr = new TList<JsonValue>();
                                    for (var log : res.logs()) {
                                        var lm = new TOrderedMap<String, JsonValue>();
                                        lm.put("t", new JsonValueNumberInt(log.timestampMs()));
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
                                            for (var e : err.errors()) {
                                                errArr.add(
                                                        ArtifactExprErrCodec.toValue(ArtifactExprErr.fromExprErr(e)));
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
                                ps.executeUpdate();
                            }
                            for (var table : new String[] {"compile_deps", "compile_data"}) {
                                try (var ps = conn.prepareStatement(
                                        "DELETE FROM " + table + " WHERE import_state = ? AND source = ?")) {
                                    ps.setLong(1, id.importState().value());
                                    ps.setLong(2, id.source().value());
                                    ps.executeUpdate();
                                }
                            }
                            try (var ps = conn.prepareStatement("INSERT OR REPLACE INTO compile_data"
                                    + " (import_state, source, path, mtime) VALUES (?, ?, ?, ?)")) {
                                for (var data : product.localData()) {
                                    ps.setLong(1, id.importState().value());
                                    ps.setLong(2, id.source().value());
                                    ps.setString(3, data.path());
                                    ps.setLong(4, data.timestampMs());
                                    ps.addBatch();
                                }
                                ps.executeBatch();
                            }
                            try (var ps = conn.prepareStatement("INSERT OR IGNORE INTO compile_deps"
                                    + " (import_state, source, dep_import_state, dep_source) VALUES (?, ?, ?, ?)")) {
                                for (var depId : product.localDeps()) {
                                    ps.setLong(1, id.importState().value());
                                    ps.setLong(2, id.source().value());
                                    ps.setLong(3, depId.importState().value());
                                    ps.setLong(4, depId.source().value());
                                    ps.addBatch();
                                }
                                ps.executeBatch();
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
