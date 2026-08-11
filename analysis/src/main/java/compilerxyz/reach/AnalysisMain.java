package compilerxyz.reach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import spoon.Launcher;
import spoon.reflect.CtModel;

/**
 * Single entry point for all Spoon-based static analysis: builds the whole-program source model
 * once, then runs the convention checks (warnings) and the reachability / single-use analysis
 * (report file). Replaces the former Error Prone checkers.
 */
public final class AnalysisMain {
  private AnalysisMain() {}

  public static void main(String[] args) throws IOException {
    Path base = Path.of(args.length > 0 ? args[0] : ".");
    Path staged = base.resolve("analysis/target/spoon-src");
    stageSources(
        base,
        staged,
        List.of(
            "util/src/main/java",
            "codegen/src/main/java",
            "runtime/src/main/java",
            "runtime/src/test/java",
            "runtime/target/generated-sources/codegen"));

    Set<String> generatedFiles =
        stagedFilesFrom(base, staged, "runtime/target/generated-sources/codegen");

    Launcher launcher = new Launcher();
    launcher.getEnvironment().setComplianceLevel(21);
    launcher.getEnvironment().setNoClasspath(true);
    launcher.getEnvironment().setCommentEnabled(false);
    launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);
    launcher.addInputResource(staged.toString());
    launcher.buildModel();
    CtModel model = launcher.getModel();

    var findings = new ConventionChecks(generatedFiles).run(model);
    printConventions(findings);
    new ReachabilityMain().run(model);

    // Convention violations fail the build. (Reachability stays a report — its findings are
    // review candidates, not hard errors.)
    if (!findings.isEmpty()) {
      throw new IllegalStateException(
          findings.size()
              + " convention violations — see analysis/target/conventions-report.txt");
    }
  }

  private static Set<String> stagedFilesFrom(Path base, Path staged, String root)
      throws IOException {
    Set<String> out = new java.util.HashSet<>();
    Path src = base.resolve(root);
    if (!Files.isDirectory(src)) return out;
    try (var walk = Files.walk(src)) {
      for (Path p : (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".java"))::iterator) {
        out.add(staged.resolve(src.relativize(p)).toAbsolutePath().normalize().toString());
      }
    }
    return out;
  }

  private static void printConventions(List<ConventionChecks.Finding> findings) throws IOException {
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (var f : findings) counts.merge(f.check(), 1, Integer::sum);
    System.out.println("=== Convention checks (" + findings.size() + " findings) ===");
    counts.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .forEach(e -> System.out.printf("  %4d  %s%n", e.getValue(), e.getKey()));
    // Full per-location detail goes to a report file to keep the build log readable.
    StringBuilder detail = new StringBuilder();
    findings.stream()
        .sorted(Comparator.comparing(ConventionChecks.Finding::file).thenComparingInt(ConventionChecks.Finding::line))
        .forEach(f -> detail.append(String.format("%s:%d [%s] %s%n", shorten(f.file()), f.line(), f.check(), f.message())));
    Path report = Path.of("analysis/target/conventions-report.txt");
    Files.createDirectories(report.getParent());
    Files.writeString(report, detail.toString());
    System.out.println("  (per-location detail in " + report + ")");
    System.out.println();
  }

  private static String shorten(String path) {
    int i = path.indexOf("spoon-src/");
    return i >= 0 ? path.substring(i + "spoon-src/".length()) : path;
  }

  private static void stageSources(Path base, Path staged, List<String> roots) throws IOException {
    if (Files.exists(staged)) {
      try (var walk = Files.walk(staged)) {
        walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
      }
    }
    Files.createDirectories(staged);
    for (String root : roots) {
      Path src = base.resolve(root);
      if (!Files.isDirectory(src)) continue;
      try (var walk = Files.walk(src)) {
        for (Path p : (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".java"))::iterator) {
          Path target = staged.resolve(src.relativize(p));
          Files.createDirectories(target.getParent());
          Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
  }
}
