package compilerxyz.main.eval.builtin;

import compilerxyz.main.eval.EvaluationLocal;
import compilerxyz.main.eval.dynamic.DynPanic;
import compilerxyz.main.gen.ListStoreInt;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;

@BuiltinFull(uniqueId = 6468L)
public final class ExecRun implements EvaluationLocal {
    private final Path root;
    private final Path fs;
    private final Path stdoutFile;
    private final Path stderrFile;
    private final Process process;
    private final String command;
    private final int[] successCodes;

    private ExecRun(
            Path root, Path fs, Path stdoutFile, Path stderrFile, Process process, String command, int[] successCodes) {
        this.root = root;
        this.fs = fs;
        this.stdoutFile = stdoutFile;
        this.stderrFile = stderrFile;
        this.process = process;
        this.command = command;
        this.successCodes = successCodes;
    }

    public static ExecRun start(
            String command,
            TList<String> args,
            TOrderedMap<String, String> env,
            TOrderedMap<String, byte[]> files,
            Opt<ListStoreInt> successCodes) {
        int[] success;
        if (successCodes.optIsSome()) {
            var store = successCodes.optUnwrap();
            success = new int[store.size()];
            for (int i = 0; i < success.length; i++) success[i] = store.get(i);
        } else {
            success = new int[] {0};
        }
        Path root;
        try {
            root = Files.createTempDirectory("allig-exec");
        } catch (IOException e) {
            throw new DynPanic("exec: cannot create sandbox directory: " + e.getMessage());
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteTree(root)));
        var fs = root.resolve("fs");
        var stdoutFile = root.resolve("stdout");
        var stderrFile = root.resolve("stderr");
        try {
            Files.createDirectory(fs);
            for (var entry : files.entrySet()) {
                var target = sandboxPath(fs, entry.getKey(), "exec");
                var parent = target.getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.write(target, entry.getValue());
            }
        } catch (IOException e) {
            throw new DynPanic("exec: cannot populate sandbox directory: " + e.getMessage());
        }
        var argv = new ArrayList<String>(args.size() + 1);
        argv.add(command);
        for (var arg : args) argv.add(arg);
        var pb = new ProcessBuilder(argv)
                .directory(fs.toFile())
                .redirectInput(ProcessBuilder.Redirect.from(Path.of("/dev/null").toFile()))
                .redirectOutput(stdoutFile.toFile())
                .redirectError(stderrFile.toFile());
        for (var entry : env.entrySet()) pb.environment().put(entry.getKey(), entry.getValue());
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new DynPanic("exec: cannot start " + command + ": " + e.getMessage());
        }
        return new ExecRun(root, fs, stdoutFile, stderrFile, process, command, success);
    }

    static Path sandboxPath(Path fs, String path, String what) {
        var rel = Path.of(path);
        if (rel.isAbsolute()) throw new DynPanic(what + ": path must be relative: " + path);
        var resolved = fs.resolve(rel).normalize();
        if (!resolved.startsWith(fs) || resolved.equals(fs)) {
            throw new DynPanic(what + ": path escapes the sandbox directory: " + path);
        }
        return resolved;
    }

    private int await() {
        int code;
        try {
            code = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DynPanic("exec: interrupted while waiting for process");
        }
        for (var ok : successCodes) {
            if (ok == code) return code;
        }
        throw new DynPanic("exec: " + command + " exited with code " + code + ":\n" + readUtf8(stderrFile));
    }

    private static String readUtf8(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DynPanic("exec: cannot read process output: " + e.getMessage());
        }
    }

    public int exitCode() {
        return await();
    }

    public String stdout() {
        await();
        return readUtf8(stdoutFile);
    }

    public String stderr() {
        await();
        return readUtf8(stderrFile);
    }

    public DirHandle dir() {
        await();
        return new DirHandle(fs);
    }

    private static void deleteTree(Path root) {
        if (!Files.exists(root)) return;
        try (var walk = Files.walk(root)) {
            for (var p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        } catch (IOException e) {
            throw new IllegalStateException("exec: cannot delete sandbox directory " + root, e);
        }
    }
}
