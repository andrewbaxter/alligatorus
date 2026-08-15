package compilerxyz.reach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import spoon.Launcher;
import spoon.reflect.CtModel;

/**
 * Single entry point for all Spoon-based static analysis. All lints are fatal: every finding from
 * every linter is summed into a single total and the build fails if that total is nonzero. To add
 * a new linter, run it and add its finding count to {@code total} — it is then automatically fatal.
 */
public final class AnalysisMain {
  private AnalysisMain() {}

  public static void main(String[] args) throws IOException {
    Path base = Path.of(args.length > 0 ? args[0] : ".");
    Path staged = base.resolve("analysis/target/spoon-src");
    if (Files.exists(staged)) {
      try (var walk = Files.walk(staged)) {
        walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
      }
    }
    Files.createDirectories(staged);
    for (String _ssRoot : List.of(
        "util/src/main/java",
        "codegen/src/main/java",
        "runtime/src/main/java",
        "runtime/src/test/java",
        "runtime/target/generated-sources/codegen",
        "analysis/src/main/java")) {
      Path _ssSrc = base.resolve(_ssRoot);
      if (!Files.isDirectory(_ssSrc)) continue;
      try (var walk = Files.walk(_ssSrc)) {
        for (Path _ssP : (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".java"))::iterator) {
          Path _ssTarget = staged.resolve(_ssSrc.relativize(_ssP));
          Files.createDirectories(_ssTarget.getParent());
          Files.copy(_ssP, _ssTarget, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }

    Set<String> generatedFiles = new HashSet<>();
    Path _sffSrc = base.resolve("runtime/target/generated-sources/codegen");
    if (Files.isDirectory(_sffSrc)) {
      try (var walk = Files.walk(_sffSrc)) {
        for (Path _sffP : (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".java"))::iterator) {
          generatedFiles.add(staged.resolve(_sffSrc.relativize(_sffP)).toAbsolutePath().normalize().toString());
        }
      }
    }

    Launcher launcher = new Launcher();
    launcher.getEnvironment().setComplianceLevel(21);
    launcher.getEnvironment().setNoClasspath(true);
    launcher.getEnvironment().setCommentEnabled(false);
    launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);
    launcher.addInputResource(staged.toString());
    launcher.buildModel();
    CtModel model = launcher.getModel();

    var conventionFindings = new ConventionChecks(generatedFiles).run(model);
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (var f : conventionFindings) counts.merge(f.check(), 1, Integer::sum);
    System.out.println("=== Convention checks (" + conventionFindings.size() + " findings) ===");
    counts.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .forEach(e -> System.out.printf("  %4d  %s%n", e.getValue(), e.getKey()));
    StringBuilder detail = new StringBuilder();
    conventionFindings.stream()
        .sorted(Comparator.comparing(ConventionChecks.Finding::file).thenComparingInt(ConventionChecks.Finding::line))
        .forEach(f -> {
          String _sfPath = f.file();
          int _sfI = _sfPath.indexOf("spoon-src/");
          String _sfShort = _sfI >= 0 ? _sfPath.substring(_sfI + "spoon-src/".length()) : _sfPath;
          detail.append(String.format("%s:%d [%s] %s%n", _sfShort, f.line(), f.check(), f.message()));
        });
    Path conventionsReport = Path.of("analysis/target/conventions-report.txt");
    Files.createDirectories(conventionsReport.getParent());
    Files.writeString(conventionsReport, detail.toString());
    System.out.println("  (per-location detail in " + conventionsReport + ")");
    System.out.println();

    int reachabilityFindings = new ReachabilityMain(generatedFiles).run(model);

    int total = conventionFindings.size() + reachabilityFindings;
    if (total > 0) {
      throw new IllegalStateException(
          total + " lint violations — see analysis/target/conventions-report.txt"
              + " and analysis/target/reachability-report.txt");
    }
  }
}
