package compilerxyz.main.langhelp;

import com.google.gson.stream.JsonWriter;
import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactJson;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.ExprSubErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.builtin.App;
import compilerxyz.main.eval.builtin.Dir;
import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.main.safejson.JsonValues;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;

public final class Main {
    private Main() {}

    private static final String USAGE =
            """
            usage: alligatorus build <path.at>
                   alligatorus build <path.at> --dir <out>
                   alligatorus run <path.at> [args...]""";

    private record MainError(Opt<QExprId> loc, TList<String> messages) {}

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println(USAGE);
            System.exit(2);
        }
        var command = args[0];
        var path = Path.of(args[1]).toAbsolutePath().normalize().toString();
        var rest = Arrays.copyOfRange(args, 2, args.length);
        switch (command) {
            case "build" -> {
                Opt<Path> dirOut;
                if (rest.length == 0) {
                    dirOut = Opt.none();
                } else if (rest.length == 2 && rest[0].equals("--dir")) {
                    dirOut = Opt.some(Path.of(rest[1]));
                } else {
                    System.err.println(USAGE);
                    System.exit(2);
                    return;
                }
                Result<Opt<Artifact>, TList<MainError>> built;
                try (var cache = new CacheReal()) {
                    built = switch (compileRoot(cache, path)) {
                        case Result.Err<Compile.CompileResult, TList<MainError>> err -> Result.err(err.error());
                        case Result.Ok<Compile.CompileResult, TList<MainError>> ok -> {
                            var artifact = ok.value().artifact();
                            if (dirOut.optIsNone()) yield Result.ok(artifact);
                            if (ok.value().artifact().optIsNone()) {
                                yield mainError("root module exports nothing; --dir expects a dir");
                            }
                            yield switch (ArtifactResolve.artifactResolveDir(
                                    cache, ok.value().artifact().optUnwrap())) {
                                case Result.Ok<Dir, String> dirOk -> {
                                    try {
                                        writeDir(dirOk.value(), dirOut.optUnwrap());
                                        yield Result.ok(artifact);
                                    } catch (IOException e) {
                                        yield mainError("cannot write " + dirOut.optUnwrap() + ": " + e.getMessage());
                                    }
                                }
                                case Result.Err<Dir, String> dirErr -> mainError(dirErr.error());
                            };
                        }
                    };
                }
                var out = new StringWriter();
                try (var w = new JsonWriter(out)) {
                    w.beginObject();
                    switch (built) {
                        case Result.Ok<Opt<Artifact>, TList<MainError>> ok -> {
                            if (ok.value().optIsSome()) {
                                w.name("artifact");
                                JsonValues.encode(
                                        w,
                                        ArtifactJson.encodeArtifact(ok.value().optUnwrap()));
                            }
                        }
                        case Result.Err<Opt<Artifact>, TList<MainError>> err -> {
                            w.name("errors").beginArray();
                            for (var e : err.error()) {
                                w.beginObject();
                                if (e.loc().optIsSome()) {
                                    var loc = e.loc().optUnwrap();
                                    w.name("source").value(loc.source().value());
                                    if (loc.expr().optIsSome())
                                        w.name("expr").value(loc.expr().optUnwrap());
                                }
                                w.name("messages").beginArray();
                                for (var m : e.messages()) w.value(m);
                                w.endArray();
                                w.endObject();
                            }
                            w.endArray();
                        }
                    }
                    w.endObject();
                } catch (IOException e) {
                    throw new AssertionError("In-memory IO failed", e);
                }
                System.out.println(out);
                switch (built) {
                    case Result.Ok<Opt<Artifact>, TList<MainError>> ok -> {}
                    case Result.Err<Opt<Artifact>, TList<MainError>> err -> System.exit(1);
                }
            }
            case "run" -> {
                Result<App, TList<MainError>> app;
                try (var cache = new CacheReal()) {
                    app = switch (compileRoot(cache, path)) {
                        case Result.Err<Compile.CompileResult, TList<MainError>> err -> Result.err(err.error());
                        case Result.Ok<Compile.CompileResult, TList<MainError>> ok -> {
                            if (ok.value().artifact().optIsNone()) {
                                yield mainError("root module exports nothing; run expects an app");
                            }
                            yield switch (ArtifactResolve.artifactResolveApp(
                                    cache, ok.value().artifact().optUnwrap())) {
                                case Result.Ok<App, String> appOk -> Result.ok(appOk.value());
                                case Result.Err<App, String> appErr -> mainError(appErr.error());
                            };
                        }
                    };
                }
                switch (app) {
                    case Result.Ok<App, TList<MainError>> ok -> System.exit(runApp(ok.value(), rest));
                    case Result.Err<App, TList<MainError>> err -> {
                        for (var e : err.error()) {
                            for (var m : e.messages()) {
                                if (e.loc().optIsSome()) {
                                    System.err.println("error at " + e.loc().optUnwrap() + ": " + m);
                                } else {
                                    System.err.println("error: " + m);
                                }
                            }
                        }
                        System.exit(1);
                    }
                }
            }
            default -> {
                System.err.println(USAGE);
                System.exit(2);
            }
        }
    }

    private static <T> Result<T, TList<MainError>> mainError(String message) {
        return Result.err(TList.of(new MainError(Opt.none(), TList.of(message))));
    }

    public static void writeDir(Dir dir, Path out) throws IOException {
        var base = out.toAbsolutePath().normalize();
        Files.createDirectories(base);
        for (var entry : dir.files().entrySet()) {
            var target = base.resolve(entry.getKey()).normalize();
            if (!target.startsWith(base)) {
                throw new IOException("dir entry escapes the output directory: " + entry.getKey());
            }
            var parent = target.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.write(target, entry.getValue());
        }
    }

    private static Result<Compile.CompileResult, TList<MainError>> compileRoot(CacheReal cache, String path) {
        var gx = GlobalContext.newContext(cache, cache, cache);
        var entry = new SourceSpecAbsolute.Local(path);
        Sources.SourceResolved.Module module;
        switch (gx.sources.sourcesGet(entry)) {
            case Result.Err<Sources.SourceResolved, String> err -> {
                return mainError(err.error());
            }
            case Result.Ok<Sources.SourceResolved, String> ok -> {
                switch (ok.value()) {
                    case Sources.SourceResolved.Module m -> module = m;
                    case Sources.SourceResolved.Bundle b -> {
                        return mainError("entry point must be a source file, not a bundle: " + path);
                    }
                    case Sources.SourceResolved.File f -> {
                        return mainError("entry point must be a module (.at or .atz): " + path);
                    }
                }
            }
        }
        return switch (Compile.compile(gx, entry, module)) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> ok -> Result.ok(ok.value());
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err -> {
                var out = new TList<MainError>();
                for (var e : err.error()) {
                    var msgs = new TList<String>();
                    for (var sub : e.errors()) {
                        switch (sub) {
                            case ExprSubErr.Opaque o -> msgs.add(o.message());
                        }
                    }
                    out.add(new MainError(Opt.some(e.loc()), msgs));
                }
                yield Result.err(out);
            }
        };
    }

    public static int runApp(App app, String[] args) {
        Path exe;
        try {
            exe = Files.createTempFile("allig-app", "");
            Files.write(exe, app.bytes());
            Files.setPosixFilePermissions(
                    exe,
                    EnumSet.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_EXECUTE));
        } catch (IOException e) {
            System.err.println("error: cannot write executable: " + e.getMessage());
            return 1;
        }
        try {
            var argv = new ArrayList<String>(args.length + 2);
            if (app.executor().optIsSome()) argv.add(app.executor().optUnwrap());
            argv.add(exe.toString());
            argv.addAll(Arrays.asList(args));
            var process = new ProcessBuilder(argv).inheritIO().start();
            return process.waitFor();
        } catch (IOException e) {
            System.err.println("error: cannot start app: " + e.getMessage());
            return 1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 1;
        } finally {
            try {
                Files.deleteIfExists(exe);
            } catch (IOException e) {
                System.err.println("warning: cannot delete " + exe + ": " + e.getMessage());
            }
        }
    }
}
