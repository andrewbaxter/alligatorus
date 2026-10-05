package compilerxyz.main.langhelp;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.builtin.App;
import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;

public final class Main {
    private Main() {}

    private static final String USAGE =
            "usage: alligatorus build <path.at>\n       alligatorus run <path.at> [args...]";

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
                if (rest.length != 0) {
                    System.err.println(USAGE);
                    System.exit(2);
                }
                try (var cache = new CacheReal()) {
                    var out = compileOrExit(cache, path);
                    System.out.println("{\"import_state\":"
                            + out.rootCompileId().importState().value()
                            + ",\"source\":"
                            + out.rootCompileId().source().value()
                            + "}");
                }
            }
            case "run" -> {
                App app;
                try (var cache = new CacheReal()) {
                    var out = compileOrExit(cache, path);
                    if (out.artifact().optIsNone()) {
                        System.err.println("error: root module exports nothing; run expects an app");
                        System.exit(1);
                        return;
                    }
                    switch (ArtifactResolve.artifactResolveApp(
                            cache, out.artifact().optUnwrap())) {
                        case Result.Ok<App, String> ok -> app = ok.value();
                        case Result.Err<App, String> err -> {
                            System.err.println("error: " + err.error());
                            System.exit(1);
                            return;
                        }
                    }
                }
                System.exit(runApp(app, rest));
            }
            default -> {
                System.err.println(USAGE);
                System.exit(2);
            }
        }
    }

    private static Compile.CompileResult compileOrExit(CacheReal cache, String path) {
        var gx = GlobalContext.newContext(cache, cache, cache);
        var entry = new SourceSpecAbsolute.Local(path);
        Sources.SourceResolved.Module module;
        switch (gx.sources.sourcesGet(entry)) {
            case Result.Err<Sources.SourceResolved, String> err -> {
                System.err.println("error: " + err.error());
                System.exit(1);
                throw new IllegalStateException("unreachable");
            }
            case Result.Ok<Sources.SourceResolved, String> ok -> {
                switch (ok.value()) {
                    case Sources.SourceResolved.Module m -> module = m;
                    case Sources.SourceResolved.Bundle b -> {
                        System.err.println("error: entry point must be a source file, not a bundle: " + path);
                        System.exit(1);
                        throw new IllegalStateException("unreachable");
                    }
                }
            }
        }
        switch (Compile.compile(gx, entry, module)) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> ok -> {
                for (var log : ok.value().logs()) {
                    System.err.println(log.message());
                }
                return ok.value();
            }
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err -> {
                for (var e : err.error()) {
                    System.err.println("error at " + e.loc() + ": " + e.errors());
                }
                System.exit(1);
                throw new IllegalStateException("unreachable");
            }
        }
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
