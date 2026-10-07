package compilerxyz.lint.docguard;

import compilerxyz.prelude.util.Opt;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GitRepo {
    private final Path root;

    private GitRepo(Path root) {
        this.root = root;
    }

    public static Opt<GitRepo> at(Path start) {
        Exec e = run(start, "rev-parse", "--show-toplevel");
        if (e.code != 0) {
            return Opt.none();
        }
        return Opt.some(new GitRepo(Path.of(e.out.strip())));
    }

    public boolean hasHead() {
        return run(root, "rev-parse", "--verify", "HEAD").code == 0;
    }

    public List<String> stagedChanged(String suffix) {
        Exec e = run(root, "diff", "--cached", "--name-only", "--diff-filter=ACMR", "--", "*" + suffix);
        return lines(e, suffix);
    }

    public List<String> workingTree(String suffix) {
        Exec e = run(root, "ls-files", "--cached", "--others", "--exclude-standard", "--", "*" + suffix);
        return lines(e, suffix);
    }

    private static List<String> lines(Exec e, String suffix) {
        if (e.code != 0) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String line : e.out.split("\n")) {
            String p = line.strip();
            if (!p.isEmpty() && p.endsWith(suffix)) {
                out.add(p);
            }
        }
        return out;
    }

    public Opt<String> parentContent(String path) {
        Exec e = run(root, "show", "HEAD:" + path);
        if (e.code != 0) {
            return Opt.none();
        }
        return Opt.some(e.out);
    }

    public String currentContent(String path) throws IOException {
        return Files.readString(root.resolve(path), StandardCharsets.UTF_8);
    }

    public String stagedContent(String path) throws IOException {
        Exec e = run(root, "show", ":" + path);
        if (e.code != 0) {
            throw new IOException("cannot read staged blob: " + path);
        }
        return e.out;
    }

    public boolean workingMatches(String path, String content) {
        try {
            return Files.readString(root.resolve(path), StandardCharsets.UTF_8).equals(content);
        } catch (IOException e) {
            return false;
        }
    }

    public void writeWorking(String path, String content) throws IOException {
        Files.writeString(root.resolve(path), content, StandardCharsets.UTF_8);
    }

    public void stageContent(String path, String content) throws IOException {
        Exec e = exec(root, content, new String[] {"hash-object", "-w", "--path", path, "--stdin"});
        if (e.code != 0) {
            throw new IOException("hash-object failed: " + path);
        }
        String hash = e.out.strip();
        Exec u = run(root, "update-index", "--cacheinfo", stagedMode(path) + "," + hash + "," + path);
        if (u.code != 0) {
            throw new IOException("update-index failed: " + path);
        }
    }

    public boolean revertIndexToParent(String path) {
        Exec e = run(root, "rev-parse", "HEAD:" + path);
        if (e.code != 0) {
            return false;
        }
        String hash = e.out.strip();
        run(root, "update-index", "--cacheinfo", stagedMode(path) + "," + hash + "," + path);
        return true;
    }

    private String stagedMode(String path) {
        Exec e = run(root, "ls-files", "--stage", "--", path);
        String out = e.out.strip();
        if (out.isEmpty()) {
            return "100644";
        }
        return out.split("\\s+")[0];
    }

    private static Exec run(Path dir, String... args) {
        return exec(dir, null, args);
    }

    private static Exec exec(Path dir, String stdin, String[] args) {
        List<String> cmd = new ArrayList<>();
        cmd.add("git");
        cmd.addAll(List.of(args));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir.toFile());
        pb.redirectErrorStream(false);
        try {
            Process p = pb.start();
            if (stdin != null) {
                try (var os = p.getOutputStream()) {
                    os.write(stdin.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                p.getOutputStream().close();
            }
            byte[] out = readAll(p.getInputStream());
            readAll(p.getErrorStream());
            int code = p.waitFor();
            return new Exec(code, new String(out, StandardCharsets.UTF_8));
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new Exec(-1, "");
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) != -1) {
            buf.write(chunk, 0, n);
        }
        return buf.toByteArray();
    }

    private record Exec(int code, String out) {}
}
