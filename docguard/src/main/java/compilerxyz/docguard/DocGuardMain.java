package compilerxyz.docguard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class DocGuardMain {
    private DocGuardMain() {}

    public static void main(String[] args) throws IOException {
        if (System.getenv("DOCGUARD_OFF") != null) {
            System.exit(0);
        }
        if (args.length == 0) {
            System.err.println("usage: docguard <scan|check|fix|strip-inline> [paths...]");
            System.exit(2);
        }
        String mode = args[0];
        List<String> rest = argsAfter(args, 1);

        switch (mode) {
            case "strip-inline" -> {
                stripInline(rest);
                System.exit(0);
            }
            case "scan" -> System.exit(scan());
            case "check" -> System.exit(perCommit(rest, false));
            case "fix" -> System.exit(perCommit(rest, true));
            default -> {
                System.err.println("docguard: unknown mode '" + mode + "'");
                System.exit(2);
            }
        }
    }

    private static int scan() throws IOException {
        Optional<GitRepo> repoOpt = GitRepo.at(Path.of("."));
        if (repoOpt.isEmpty()) {
            System.out.println("docguard: not a git repository; skipping.");
            return 0;
        }
        GitRepo repo = repoOpt.get();
        List<Violation> violations = new ArrayList<>();
        for (String path : repo.tracked(".java")) {
            String current;
            try {
                current = repo.currentContent(path);
            } catch (IOException e) {
                continue;
            }
            violations.addAll(new JavaGuard(path).check(Optional.empty(), current, false));
        }
        violations.forEach(v -> System.out.println(v.render()));
        return violations.isEmpty() ? 0 : 1;
    }

    private static int perCommit(List<String> paths, boolean fix) throws IOException {
        Optional<GitRepo> repoOpt = GitRepo.at(Path.of("."));
        if (repoOpt.isEmpty()) {
            System.out.println("docguard: not a git repository; skipping.");
            return 0;
        }
        GitRepo repo = repoOpt.get();
        boolean hasHead = repo.hasHead();
        Set<String> restrict = new LinkedHashSet<>(paths);
        List<Violation> violations = new ArrayList<>();
        List<String> fixedFiles = new ArrayList<>();

        for (String path : select(repo.stagedChanged(".java"), restrict)) {
            Optional<String> parent = hasHead ? repo.parentContent(path) : Optional.empty();
            String staged;
            try {
                staged = repo.stagedContent(path);
            } catch (IOException e) {
                continue;
            }
            JavaGuard guard = new JavaGuard(path);
            if (fix) {
                boolean workingInSync = repo.workingMatches(path, staged);
                JavaGuard.FixResult fr = guard.fix(parent, staged, true);
                if (fr.changed()) {
                    repo.stageContent(path, fr.output());
                    if (workingInSync) {
                        repo.writeWorking(path, fr.output());
                    }
                    fixedFiles.add(path);
                }
                violations.addAll(fr.residual());
            } else {
                violations.addAll(guard.check(parent, staged, true));
            }
        }

        for (String path : select(repo.stagedChanged(".md"), restrict)) {
            Optional<String> parent = hasHead ? repo.parentContent(path) : Optional.empty();
            String staged;
            try {
                staged = repo.stagedContent(path);
            } catch (IOException e) {
                continue;
            }
            if (fix && DocFileGuard.shouldRevert(parent, staged)) {
                boolean workingInSync = repo.workingMatches(path, staged);
                repo.revertIndexToParent(path);
                if (workingInSync && parent.isPresent()) {
                    repo.writeWorking(path, parent.get());
                }
                fixedFiles.add(path);
            } else {
                violations.addAll(DocFileGuard.check(path, parent, staged));
            }
        }

        fixedFiles.forEach(f -> System.out.println("docguard: fixed " + f));
        violations.forEach(v -> System.out.println(v.render()));
        return violations.isEmpty() ? 0 : 1;
    }

    private static void stripInline(List<String> files) {
        for (String f : files) {
            Path p = Path.of(f);
            if (!f.endsWith(".java") || !Files.isRegularFile(p)) {
                continue;
            }
            String content;
            try {
                content = Files.readString(p, StandardCharsets.UTF_8);
            } catch (IOException e) {
                continue;
            }
            JavaGuard.FixResult fr = new JavaGuard(f).stripComments(content);
            if (fr.changed()) {
                try {
                    Files.writeString(p, fr.output(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    continue;
                }
            }
        }
    }

    private static List<String> select(List<String> changed, Set<String> restrict) {
        if (restrict.isEmpty()) {
            return changed;
        }
        List<String> out = new ArrayList<>();
        for (String c : changed) {
            if (restrict.contains(c)) {
                out.add(c);
            }
        }
        return out;
    }

    private static List<String> argsAfter(String[] args, int from) {
        List<String> out = new ArrayList<>();
        for (int i = from; i < args.length; i++) {
            out.add(args[i]);
        }
        return out;
    }
}
