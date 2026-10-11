package compilerxyz.lint.docguard;

import compilerxyz.prelude.util.Opt;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DocGuardMain {
    private DocGuardMain() {}

    public static void main(String[] args) throws IOException {
        List<String> rest = new ArrayList<>(Arrays.asList(args));
        String against = "HEAD";
        if (rest.size() >= 2 && rest.get(0).equals("--against")) {
            against = rest.get(1);
            rest = new ArrayList<>(rest.subList(2, rest.size()));
        }
        if (rest.isEmpty()) {
            System.err.println("usage: docguard [--against REV] <scan|check|fix|strip-inline> [paths...]");
            System.exit(2);
        }
        String mode = rest.remove(0);

        switch (mode) {
            case "strip-inline" -> {
                for (String f : rest) {
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
                System.exit(0);
            }
            case "scan" -> {
                Opt<GitRepo> repoOpt = GitRepo.at(Path.of("."), against);
                if (repoOpt.optIsNone()) {
                    System.out.println("docguard: not a git repository; skipping.");
                    System.exit(0);
                }
                GitRepo repo = repoOpt.optUnwrap();
                boolean hasHead = hasAgainst(repo, against);
                List<Violation> violations = new ArrayList<>();
                for (String path : repo.workingTree(".java")) {
                    String current;
                    try {
                        current = repo.currentContent(path);
                    } catch (IOException e) {
                        continue;
                    }
                    Opt<String> parent = hasHead ? repo.parentContent(path) : Opt.none();
                    violations.addAll(new JavaGuard(path).check(parent, current));
                }
                violations.forEach(v -> System.out.println(v.render()));
                System.exit(violations.isEmpty() ? 0 : 1);
            }
            case "check" -> System.exit(perCommit(against, rest, false));
            case "fix" -> System.exit(perCommit(against, rest, true));
            default -> {
                System.err.println("docguard: unknown mode '" + mode + "'");
                System.exit(2);
            }
        }
    }

    private static boolean hasAgainst(GitRepo repo, String against) {
        boolean has = repo.hasAgainst();
        if (!has && !against.equals("HEAD")) {
            System.err.println("docguard: no commit named '" + against + "'");
            System.exit(2);
        }
        return has;
    }

    private static int perCommit(String against, List<String> paths, boolean fix) throws IOException {
        Opt<GitRepo> repoOpt = GitRepo.at(Path.of("."), against);
        if (repoOpt.optIsNone()) {
            System.out.println("docguard: not a git repository; skipping.");
            return 0;
        }
        GitRepo repo = repoOpt.optUnwrap();
        boolean hasHead = hasAgainst(repo, against);
        Set<String> restrict = new LinkedHashSet<>(paths);
        List<Violation> violations = new ArrayList<>();
        List<String> fixedFiles = new ArrayList<>();

        for (String path : select(repo.stagedChanged(".java"), restrict)) {
            Opt<String> parent = hasHead ? repo.parentContent(path) : Opt.none();
            String staged;
            try {
                staged = repo.stagedContent(path);
            } catch (IOException e) {
                continue;
            }
            JavaGuard guard = new JavaGuard(path);
            if (fix) {
                boolean workingInSync = repo.workingMatches(path, staged);
                JavaGuard.FixResult fr = guard.fix(parent, staged);
                if (fr.changed()) {
                    repo.stageContent(path, fr.output());
                    if (workingInSync) {
                        repo.writeWorking(path, fr.output());
                    }
                    fixedFiles.add(path);
                }
                violations.addAll(fr.residual());
            } else {
                violations.addAll(guard.check(parent, staged));
            }
        }

        for (String path : select(repo.stagedChanged(".md"), restrict)) {
            Opt<String> parent = hasHead ? repo.parentContent(path) : Opt.none();
            String staged;
            try {
                staged = repo.stagedContent(path);
            } catch (IOException e) {
                continue;
            }
            boolean shouldRevert = parent.optIsSome() && !parent.optUnwrap().equals(staged);
            if (fix && shouldRevert) {
                boolean workingInSync = repo.workingMatches(path, staged);
                repo.revertIndexToParent(path);
                if (workingInSync) {
                    repo.writeWorking(path, parent.optUnwrap());
                }
                fixedFiles.add(path);
            } else if (parent.optIsNone()) {
                violations.add(
                        new Violation(path, 0, "doc-added", "new tracked doc file (not reverted; review manually)"));
            } else if (!parent.optUnwrap().equals(staged)) {
                violations.add(new Violation(path, 0, "doc-modified", "tracked doc file modified vs merge-base"));
            }
        }

        fixedFiles.forEach(f -> System.out.println("docguard: fixed " + f));
        violations.forEach(v -> System.out.println(v.render()));
        return violations.isEmpty() ? 0 : 1;
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
}
