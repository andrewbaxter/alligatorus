package compilerxyz.main.eval.builtin;

import compilerxyz.main.eval.EvaluationLocal;
import compilerxyz.main.eval.dynamic.DynPanic;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@BuiltinFull(uniqueId = 6469L)
public final class DirHandle implements EvaluationLocal {
    private final Path root;

    public DirHandle(Path root) {
        this.root = root;
    }

    public FileHandle open(String path) {
        var target = ExecRun.sandboxPath(root, path, "dir.open");
        if (!Files.isRegularFile(target)) throw new DynPanic("dir.open: file not found: " + path);
        try {
            return new FileHandle(Files.readAllBytes(target));
        } catch (IOException e) {
            throw new DynPanic("dir.open: cannot read " + path + ": " + e.getMessage());
        }
    }

    public boolean exists(String path) {
        return Files.exists(ExecRun.sandboxPath(root, path, "dir.exists"));
    }

    public TList<String> list(String path) {
        var target = path.isEmpty() ? root : ExecRun.sandboxPath(root, path, "dir.list");
        if (!Files.isDirectory(target)) throw new DynPanic("dir.list: directory not found: " + path);
        var out = new TList<String>();
        try (var entries = Files.list(target)) {
            for (var entry : entries.sorted().toList()) {
                out.add(entry.getFileName().toString());
            }
        } catch (IOException e) {
            throw new DynPanic("dir.list: cannot list " + path + ": " + e.getMessage());
        }
        return out;
    }
}
