package compilerxyz.main.eval.builtin;

import compilerxyz.main.eval.EvaluationLocal;
import compilerxyz.main.eval.dynamic.DynPanic;
import compilerxyz.prelude.annotations.BuiltinFull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@BuiltinFull(uniqueId = 6468L)
public final class ExecRun implements EvaluationLocal {
    private final Path fs;
    private final Path stdoutFile;
    private final Path stderrFile;
    private final Process process;
    private final String command;
    private final int[] successCodes;

    ExecRun(Path fs, Path stdoutFile, Path stderrFile, Process process, String command, int[] successCodes) {
        this.fs = fs;
        this.stdoutFile = stdoutFile;
        this.stderrFile = stderrFile;
        this.process = process;
        this.command = command;
        this.successCodes = successCodes;
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
}
